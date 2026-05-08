package com.zhidian.service.impl;

import com.alibaba.fastjson.JSON;
import com.zhidian.entity.LocalMessage;
import com.zhidian.entity.Orders;
import com.zhidian.mapper.OrderMapper;
import com.zhidian.service.MessageHandlerService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @Author 吴汇明
 * @School 绥化学院
 * @CreateTime
 */
@Service
@Slf4j
public class OrderTimeoutMessageHandler implements MessageHandlerService {
    @Autowired
    private OrderMapper orderMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    public void handleMessage(LocalMessage message) {
        try {
            Map content = JSON.parseObject(message.getContent(), Map.class);
            Long orderId = Long.valueOf(content.get("orderId").toString());
            String key="order:paid:"+orderId;
            if(stringRedisTemplate.hasKey(key)){
                log.info("订单已支付，跳过超时处理，订单ID: {}", orderId);
                return;
            }
            Orders order = orderMapper.getById(orderId);
            if(order==null){
                log.info("订单不存在，订单ID: {}", orderId);
                return;
            }
            if(!order.getStatus().equals(Orders.PENDING_PAYMENT)){
                log.info("订单状态不是待支付，跳过超时处理，订单ID: {}", orderId);
                return;
            }
            // TODO 乐观锁更新订单状态
            //int rows = orderMapper.updateOptimize(order);
            /*if (rows > 0) {
                log.info("订单超时处理成功，订单ID: {}", orderId);
            } else {
                throw new RuntimeException("更新订单状态失败");
            }*/
        } catch (Exception e) {
            log.error("处理订单超时消息失败，消息ID: {}, 错误: {}", message.getId(), e.getMessage());
            throw new RuntimeException("处理消息失败", e);
        }
    }

    public String getSupportedMessageType() {
        return LocalMessage.TYPE_ORDER_TIMEOUT;
    }
}