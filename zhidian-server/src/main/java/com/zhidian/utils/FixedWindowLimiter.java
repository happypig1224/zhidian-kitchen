package com.zhidian.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * @author HappyPig
 * @version 1.0
 * @since 2026/3/31 21:32
 */
@Component
public class FixedWindowLimiter {
    @Autowired
    private StringRedisTemplate redisTemplate;
    private final String LIMIT_KEY = "fix:limit";
    // 时间段内最大请求数
    private final int MAX_REQUESTS = 1500;
    // 时间窗口大小（毫秒）
    private final long WINDOWS_SIZE_IN_MILLISECONDS = 1000;

    public boolean tryAcquire() {
        Long counts = redisTemplate.opsForValue().increment(LIMIT_KEY);
        if (counts == 1) {
            // 第一次请求，设置过期时间为时间窗口大??
            redisTemplate.expire(LIMIT_KEY, WINDOWS_SIZE_IN_MILLISECONDS, TimeUnit.MILLISECONDS);
        }
        return counts <= MAX_REQUESTS;
    }
}
