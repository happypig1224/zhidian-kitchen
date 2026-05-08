package com.zhidian.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.zhidian.constant.MessageConstant;
import com.zhidian.context.BaseContext;
import com.zhidian.dto.*;
import com.zhidian.entity.*;
import com.zhidian.exception.AddressBookBusinessException;
import com.zhidian.exception.OrderBusinessException;
import com.zhidian.exception.ShoppingCartBusinessException;
import com.zhidian.mapper.*;
import com.zhidian.result.PageResult;
import com.zhidian.service.LocalMessageService;
import com.zhidian.service.OrderService;
import com.zhidian.utils.MemoryCacheUtil;
import com.zhidian.vo.OrderPaymentVO;
import com.zhidian.vo.OrderStatisticsVO;
import com.zhidian.vo.OrderSubmitVO;
import com.zhidian.vo.OrderVO;
import com.zhidian.websocket.WebSocketServer;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional(rollbackFor = Exception.class)
public class OrderServiceImpl implements OrderService {
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WebSocketServer webSocketServer;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private RocketMQTemplate rocketMQTemplate;
    @Autowired
    // 编程式事务
    private TransactionTemplate transactionTemplate;
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        //购物车为空,地址未选,超出配送范围抛异常
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        //获取购物车列表
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);
        if (shoppingCartList == null || shoppingCartList.isEmpty()) {
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }
        // 重复提交校验
        String submitKey = "order:submit:" + userId + ":" + generateCartHash(shoppingCartList, addressBook);
        Boolean isSubmit = stringRedisTemplate.opsForValue().setIfAbsent(submitKey, "1", 15, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(isSubmit)) {
            throw new OrderBusinessException(MessageConstant.ORDER_REPEAT_SUBMIT);
        }

        //无异常状态
        //1.更新订单表数据
        Orders order = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, order);
        //地址
        order.setAddress(addressBook.getDetail());
        //订单号,UUID防止重复
        order.setNumber(UUID.randomUUID().toString().replace("-", ""));
        //订单状态
        order.setStatus(Orders.PENDING_PAYMENT);
        //支付状态
        order.setPayStatus(Orders.UN_PAID);
        //手机号用户名
        order.setUserName(addressBook.getConsignee());
        order.setPhone(addressBook.getPhone());
        order.setUserId(userId);
        order.setOrderTime(LocalDateTime.now());
        //向订单表插入数据
        Boolean result = transactionTemplate.execute(transactionStatus -> {
            orderMapper.insert(order);
            //2.更新详细订单表的数据
            List<OrderDetail> orderDetailList = new ArrayList<>();
            shoppingCartList.forEach(cart -> {
                OrderDetail orderDetail = new OrderDetail();
                BeanUtils.copyProperties(cart, orderDetail);
                orderDetail.setOrderId(order.getId());
                orderDetailList.add(orderDetail);
            });
            orderDetailMapper.insertBatch(orderDetailList);
            return true;
        });
        if (Boolean.FALSE.equals(result)) {
            throw new OrderBusinessException(MessageConstant.ORDER_SUBMIT_FAILED);
        }
        //封装结果返回
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(order.getId())
                .orderNumber(order.getNumber())
                .orderAmount(order.getAmount())
                .orderTime(order.getOrderTime()).build();
        // 3. 发送订单超时延迟消息
        //sendOrderTimeoutMessage(order.getId());
        // 采用RocketMQ 延迟消息
        // 3.发送20分钟的延迟消息到MQ
        OrderMessage message =OrderMessage.builder()
                .orderId(order.getId())
                .userId(userId)
                .build();
        Message<OrderMessage> orderMsg = MessageBuilder.withPayload(message).build();
        SendResult sendResult = rocketMQTemplate.syncSend("order-timeout-topic", orderMsg, 3000, 15);
        if (!SendStatus.SEND_OK.equals(sendResult.getSendStatus())) {
            log.error("发送订单超时消息失败，订单ID: {}, 状态: {}", order.getId(), sendResult.getSendStatus());
            throw new OrderBusinessException("订单创建成功，但超时监控设置失败，请稍后重试");
        }
        return orderSubmitVO;
    }

    /**
     * 生成购物车哈希值
     *
     * @param shoppingCartList 购物车列表
     * @return 哈希值字符串
     */
    private String generateCartHash(List<ShoppingCart> shoppingCartList, AddressBook addressBook) {
        try {
            // 使用稳定的JSON序列化（字段排序）
            String cartJson = JSON.toJSONString(shoppingCartList,SerializerFeature.MapSortField) ;
            // 地址
            String addressJson = JSON.toJSONString(addressBook,SerializerFeature.MapSortField);
            // 合并购物车和地址JSON
            String combinedJson = cartJson + addressJson;
            // 直接返回MD5哈希值（32位十六进制）
            return DigestUtils.md5DigestAsHex((combinedJson).getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            // 降级方案：使用UUID
            return UUID.randomUUID().toString().replace("-", "");
        }
    }


/*
    private void sendOrderTimeoutMessage(Long orderId) {
        Map<String, Object> content = new HashMap<>();
        content.put("orderId", orderId);
        content.put("timeoutMinutes", 15);
        localMessageService.sendDelayMessage(orderId, LocalMessage.TYPE_ORDER_TIMEOUT, content, 15);
        log.info("发送订单超时延迟消息，订单ID: {}", orderId);
    }

    private void cancelOrderTimeoutMessage(Long orderId) {
        localMessageService.cancelMessage(orderId, LocalMessage.TYPE_ORDER_TIMEOUT);
        log.info("取消订单超时消息，订单ID: {}", orderId);
    }
*/


    /**
     * 处理订单支付请求
     *
     * @param ordersPaymentDTO 支付请求DTO
     * @return 支付VO
     * @throws Exception 支付异常
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);
        // 开发环境模拟支付，跳过真实微信支付
        // 调用微信支付接口，生成预支付交易单
        // 模拟支付返回数据
        OrderPaymentVO vo = new OrderPaymentVO();
        vo.setNonceStr("mock_nonce_str");
        vo.setPaySign("mock_pay_sign");
        vo.setTimeStamp(String.valueOf(System.currentTimeMillis() / 1000));
        vo.setSignType("RSA");
        vo.setPackageStr("prepay_id=mock_prepay_id");

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(ordersPaymentDTO.getOrderNumber());

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
        Map map = new HashMap();
        map.put("type", 1);
        map.put("orderId", ordersDB.getId());
        map.put("content", "订单号:" + ordersDB.getNumber());
        //来单提醒
        webSocketServer.sendToAllClient(JSON.toJSONString(map));
        //打上支付成功标记String key="order:paid:"+orderId;
        String key = "order_paid:" + ordersDB.getId();
        // 支付时间，金额，方式，流水号
        String value = vo.getTimeStamp() + ":" + ordersDB.getAmount() + ":" + ordersDB.getPayMethod() + ":" + user.getPhone();
        stringRedisTemplate.opsForValue().set(key, value, 30, TimeUnit.MINUTES);
        return vo;
    }


    @Override
    public PageResult pageQueryUser(int pageNum, int pageSize, Integer status) {
        // 设置分页
        PageHelper.startPage(pageNum, pageSize);
        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        ordersPageQueryDTO.setStatus(status);
        // 分页条件查询
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);
        List<OrderVO> list = new ArrayList();
        // 查询出订单明细，并封装入OrderVO进行响应
        if (page != null && page.getTotal() > 0) {
            for (Orders orders : page) {
                Long orderId = orders.getId();// 订单id
                // 查询订单明细
                List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orderId);
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                orderVO.setOrderDetailList(orderDetails);
                list.add(orderVO);
            }
        }
        return new PageResult(page.getTotal(), list);
    }

    @Override
    public OrderVO getDetailByOrderId(Long id) {
        Orders orders = orderMapper.getById(id);
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetailList);
        return orderVO;

    }

    @Override
    public void cancelOrder(Long id) {
        Orders order = orderMapper.getById(id);
        if (order == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if (order.getStatus() >= Orders.TO_BE_CONFIRMED) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        order.setStatus(Orders.CANCELLED);
        order.setCancelReason("用户取消订单");
        order.setCancelTime(LocalDateTime.now());
        orderMapper.update(order);
    }

    @Override
    public void repetition(Long id) {
        // 查询当前用户id
        Long userId = BaseContext.getCurrentId();

        // 根据订单id查询当前订单详情
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);

        // 将订单详情对象转换为购物车对象
        List<ShoppingCart> shoppingCartList = orderDetailList.stream().map(x -> {
            ShoppingCart shoppingCart = new ShoppingCart();
            // 将原订单详情里面的菜品信息重新复制到购物车对象中
            //排除id主键
            BeanUtils.copyProperties(x, shoppingCart, "id");
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());
            return shoppingCart;
        }).collect(Collectors.toList());

        // 将购物车对象批量添加到数据库
        shoppingCartMapper.insertBatch(shoppingCartList);
    }

    /**
     * 搜索订单
     *
     * @param pageQueryDTO
     * @return
     */
    public PageResult OrdersSearch(OrdersPageQueryDTO pageQueryDTO) {
        PageHelper.startPage(pageQueryDTO.getPage(), pageQueryDTO.getPageSize());
        Page<Orders> page = orderMapper.pageQuery(pageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    @Override
    public OrderStatisticsVO getOrderStatistics() {
        List<Orders> ordersList = orderMapper.getAll();
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        //待接单数量
        Integer toBeConfirmed = 0;
        //待派送数量
        Integer confirmed = 0;
        //派送中数量
        Integer deliveryInProgress = 0;
        for (Orders orders : ordersList) {
            if (orders.getStatus() == Orders.TO_BE_CONFIRMED) {
                toBeConfirmed++;
            } else if (orders.getStatus() == Orders.CONFIRMED) {
                confirmed++;
            } else if (orders.getStatus() == Orders.DELIVERY_IN_PROGRESS) {
                deliveryInProgress++;
            }
        }
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);
        return orderStatisticsVO;
    }

    @Override
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        Orders orders = orderMapper.getById(ordersConfirmDTO.getId());
        orders.setStatus(Orders.CONFIRMED);
        orderMapper.update(orders);
    }

    @Override
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) {
        Orders orders = orderMapper.getById(ordersRejectionDTO.getId());
        orders.setCancelReason(ordersRejectionDTO.getRejectionReason());
        orders.setStatus(Orders.CANCELLED);
        orderMapper.update(orders);
    }

    @Override
    public void cancelOrderAdmin(OrdersCancelDTO ordersCancelDTO) {
        Orders orders = orderMapper.getById(ordersCancelDTO.getId());
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelTime(LocalDateTime.now());
        orders.setCancelReason(ordersCancelDTO.getCancelReason());
        orderMapper.update(orders);
    }

    @Override
    public void delivery(Long id) {
        Orders orders = orderMapper.getById(id);
        orders.setStatus(Orders.DELIVERY_IN_PROGRESS);
        orderMapper.update(orders);
    }

    @Override
    public void complete(Long id) {
        //设置送达时间
        Orders orders = orderMapper.getById(id);
        orders.setStatus(Orders.COMPLETED);
        orders.setDeliveryTime(LocalDateTime.now());
        orderMapper.update(orders);
    }

    @Override
    public void reminder(Long id) {
        Orders orders = orderMapper.getById(id);
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        Map map = new HashMap();
        map.put("type", 2);
        map.put("orderId", orders.getId());
        map.put("content", "订单号:" + orders.getNumber());
        //催单
        webSocketServer.sendToAllClient(JSON.toJSONString(map));

    }


    /**
     * 生成模拟签名
     *
     * @param timeStamp
     * @param nonceStr
     * @param packageStr
     * @return
     */
    private String generateMockSign(String timeStamp, String nonceStr, String packageStr) {
        // 简单的模拟签名生成逻辑
        String signStr = timeStamp + nonceStr + packageStr;
        return Base64.getEncoder().encodeToString(
                (signStr + "mock_key").getBytes()
        );
    }

}