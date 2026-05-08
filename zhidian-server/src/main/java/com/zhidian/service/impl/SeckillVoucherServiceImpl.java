package com.zhidian.service.impl;

import com.zhidian.context.BaseContext;
import com.zhidian.dto.SeckillMessage;
import com.zhidian.entity.SeckillOrder;
import com.zhidian.entity.SeckillVoucher;
import com.zhidian.mapper.SeckillOrderMapper;
import com.zhidian.result.Result;
import com.zhidian.service.SeckillOrderService;
import com.zhidian.service.SeckillVoucherService;
import com.zhidian.mapper.SeckillVoucherMapper;
import com.zhidian.utils.MemoryCacheUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.messaging.Message;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scripting.ScriptSource;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.zhidian.utils.RedisConstant.*;
import static com.zhidian.utils.RocketMQConstant.SECKILL_VOUCHER_TAG;
import static com.zhidian.utils.RocketMQConstant.SECKILL_VOUCHER_TOPIC;

/**
 * @author 33046
 * @description 针对表【seckill_voucher(秒杀优惠券表)】的数据库操作Service实现
 * @createDate 2026-03-11 16:54:18
 */
@Slf4j
@Service
public class SeckillVoucherServiceImpl implements SeckillVoucherService {

    @Autowired
    private SeckillVoucherMapper mapper;
    @Autowired
    private SeckillOrderMapper seckillOrderMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    private DefaultRedisScript<Long> redisScript;
    @Autowired
    private RocketMQTemplate rocketMQTemplate;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private MemoryCacheUtil localCache;

    public SeckillVoucherServiceImpl() {
        redisScript = new DefaultRedisScript<>();
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("seckill.lua")));
        redisScript.setResultType(Long.class);
    }

    /**
     * 秒杀优惠券发放
     *
     * @param seckillVoucher
     */
    public Result<String> issueVoucher(SeckillVoucher seckillVoucher) {
        seckillVoucher.setVoucherId(System.currentTimeMillis());
        seckillVoucher.setCreateTime(LocalDateTime.now());
        int result = mapper.issueVoucher(seckillVoucher);
        if (result > 0) {
            // 计算从当前时间到开始时间的时间差（分钟），再加上 30 分钟作为缓存过期时间
            long beginTimeSeconds = seckillVoucher.getEndTime().atZone(java.time.ZoneId.systemDefault()).toEpochSecond();
            long currentTimeSeconds = System.currentTimeMillis() / 1000;
            long expireMinutes = (beginTimeSeconds - currentTimeSeconds) / 60L;

            // 确保过期时间不为负数
            if (expireMinutes < 0) {
                expireMinutes = 30; // 如果活动已开始，设置默认 30 分钟过期
            }

            String voucherKey = SECKILL_VOUCHER_KEY + seckillVoucher.getId();
            Integer stock = seckillVoucher.getStock();
            log.info("库存{},key{}", stock, voucherKey);
            stringRedisTemplate.opsForValue().set(voucherKey, String.valueOf(stock), expireMinutes, TimeUnit.MINUTES);
            return Result.success("发放成功");
        } else {
            return Result.error("发放失败");
        }
    }
    /**
     * 秒杀
     *
     * @param voucherId
     * @return
     */
    @Override
    public Result<String> seckill(Long voucherId) {
        String stockKey = SECKILL_VOUCHER_KEY + voucherId;
        Long userId = BaseContext.getCurrentId();
        String userKey = SECKILL_USER_KEY + userId + ":" + voucherId;
        try {
            Long result = stringRedisTemplate.execute(redisScript, Arrays.asList(stockKey, userKey));
            if (result == 1) {
                // 秒杀成功 -> 发送 MQ 消息 (异步解耦)
                SeckillMessage msg = SeckillMessage.builder().userId(userId).voucherId(voucherId).build();
                rocketMQTemplate.sendOneWay(SECKILL_VOUCHER_TOPIC + ":" + SECKILL_VOUCHER_TAG, msg);
                return Result.success("秒杀成功");
            } else if (result == -1) {
                // 库存不足 (Lua 脚本直接返回，耗时 < 2ms)
                return Result.error("库存不足");
            } else if (result == -2) {
                // 重复购买
                return Result.error("您已领取过该优惠券");
            } else {
                return Result.error("秒杀失败");
            }
        } catch (Exception e) {
            log.error("秒杀异常", e);
            // TODO 极端情况下，可以记录到本地数据库或文件，由定时任务补偿
            return Result.error("系统繁忙");
        }
    }

    @Override
    public Result<List<SeckillVoucher>> queryVoucher() {
        List<SeckillVoucher> seckillVoucher = mapper.queryVoucher();
        return Result.success(seckillVoucher);
    }

    @Override
    public Result<List<SeckillOrder>> myVoucherList() {
        Long userId = BaseContext.getCurrentId();
        List<SeckillOrder> seckillOrderList = seckillOrderMapper.selectList(userId);
        return seckillOrderList != null ? Result.success(seckillOrderList) : Result.success(Collections.emptyList());
    }

    @Async("seckillAsyncExecutor")
    public void createVoucherOrder(Long voucherId, Long userId) {
        // 创建秒杀优惠卷订单
        SeckillOrder seckillOrder = SeckillOrder.builder()
                .userId(userId)
                .voucherId(voucherId)
                .orderTime(LocalDateTime.now())
                .status(1)
                .createTime(LocalDateTime.now())
                .build();
        seckillOrderMapper.save(seckillOrder);
        // 更新数据库库存（与 Redis 保持一致）
        int result = mapper.decreaseStock(voucherId);
        if (result == 0) {
            log.error("数据库库存扣减失败，user{},voucherId{}", userId, voucherId);
            // 这里可以添加补偿逻辑，比如记录异常日志、发送告警等
        } else {
            log.info("数据库库存扣减成功，user{},voucherId{}", userId, voucherId);
        }
    }
}