package com.zhidian.rocketmq;

import com.zhidian.config.RocketMQConfig;
import com.zhidian.dto.SeckillMessage;
import com.zhidian.exception.SeckillOrderCreateFailException;
import com.zhidian.mapper.SeckillVoucherMapper;
import com.zhidian.service.SeckillOrderService;
import com.zhidian.service.SeckillVoucherService;
import com.zhidian.utils.RocketMQConstant;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

import static com.zhidian.utils.RedisConstant.SECKILL_ORDER_KEY;

/**
 * @author HappyPig
 * @version 1.0
 * @since 2026/3/23 23:43
 */
@Component
@Slf4j
@RocketMQMessageListener(
        topic = RocketMQConstant.SECKILL_VOUCHER_TOPIC,
        selectorExpression = RocketMQConstant.SECKILL_VOUCHER_TAG,
        consumerGroup = RocketMQConstant.CONSUMER_GROUP,
        messageModel = MessageModel.CLUSTERING,
        consumeThreadNumber = 8,   // 最小线程数 8
        consumeThreadMax = 16,  // 最大线程数 16
        maxReconsumeTimes = 3   // 最大重试次数 3
)
public  class SeckillConsumer implements RocketMQListener<SeckillMessage> {
    @Autowired
    private SeckillOrderService seckillOrderService;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private SeckillVoucherMapper mapper;

    @Override
    public void onMessage(SeckillMessage seckillMessage) {
        Long userId = seckillMessage.getUserId();
        Long voucherId = seckillMessage.getVoucherId();
        String mqKey=SECKILL_ORDER_KEY + userId + ":" + voucherId;
        
        // 消息幂等性处理：使用 Redis 锁确保每个用户每个优惠券只能创建一次订单
        Boolean ifAbsent = redisTemplate.opsForValue().setIfAbsent(mqKey, "1", 15, TimeUnit.MINUTES);
        if (Boolean.FALSE.equals(ifAbsent)) {
            log.info("秒杀订单已存在:{}", seckillMessage);
            return;
        }
        
        log.info("收到秒杀订单:{}", seckillMessage);
        try {
            // 异步消费流程：创建订单、扣减 DB 库存
            // 这里可以承受较高的延迟，只要最终一致性即可
            int rows = seckillOrderService.createOrder(seckillMessage);
            if (rows > 0) {
                // 扣减 DB 库存
                int result = mapper.decreaseStock(voucherId);
                if (result > 0) {
                    log.info("秒杀成功：userId={}, voucherId={}", userId, voucherId);
                } else {
                    log.error("数据库库存扣减失败：userId={}, voucherId={}", userId, voucherId);
                    // 抛出异常触发 RocketMQ 重试机制
                    throw new SeckillOrderCreateFailException("数据库库存扣减失败");
                }
            } else {
                // 创建订单失败，触发 RocketMQ 的重试机制
                log.error("创建订单失败：userId={}, voucherId={}", userId, voucherId);
                throw new SeckillOrderCreateFailException("创建订单失败");
            }
        } catch (SeckillOrderCreateFailException e) {
            // 业务异常，直接抛出触发重试
            log.warn("秒杀订单创建失败，将触发重试：{}", e.getMessage());
            throw new RuntimeException("消费失败，触发重试", e);
        } catch (Exception e) {
            // 系统异常，打印堆栈并触发重试
            log.error("消费过程异常，userId={}, voucherId={}", userId, voucherId, e);
            throw new RuntimeException("消费失败，触发重试", e);
        }
    }
    // 创建实例时打印日志
    @PostConstruct
    public void init() {
        log.info("秒杀订单消费者启动...");
    }
}