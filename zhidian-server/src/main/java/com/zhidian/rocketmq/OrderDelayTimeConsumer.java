package com.zhidian.rocketmq;

import com.zhidian.dto.OrderMessage;
import com.zhidian.entity.Orders;
import com.zhidian.mapper.OrderMapper;
import com.zhidian.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * @author HappyPig
 * @version 1.0
 * @since 2026/3/25 12:25
 */
@Slf4j
@Component
@RocketMQMessageListener(topic = "order-timeout-topic",consumerGroup = "order-delay-time-consumer")
public class OrderDelayTimeConsumer implements RocketMQListener<OrderMessage> {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public void onMessage(OrderMessage orderMessage) {
        Long orderId = orderMessage.getOrderId();

        // 1. 优先检查 Redis 标记
        String paidFlag = stringRedisTemplate.opsForValue().get("order_paid:" + orderId);
        if (paidFlag != null) {
            log.info("订单已支付，忽略超时消息，订单ID: {}", orderId);
            return;
        }

        // 2. 执行数据库乐观锁更新
        int rows = orderMapper.updateOptimize(orderId);
        if (rows > 0) {
            log.info("订单超时取消成功，订单ID: {}", orderId);
        } else {
            // 此时才认为是真正的异常或重复消费，或者仅仅是状态已变
            Orders order = orderMapper.getById(orderId);
            if (order != null && order.getPayStatus() == Orders.PAID) {
                log.info("订单已支付（数据库确认），忽略超时消息，订单ID: {}", orderId);
            } else {
                log.warn("订单取消失败，可能订单不存在或状态已变更，订单ID: {}", orderId);
            }
        }
    }
}
