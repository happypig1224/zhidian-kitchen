package com.zhidian.utils;

/**
 * @author HappyPig
 * @version 1.0
 * @since 2026/3/31 21:08
 */
public class TokenBucketLimiter {
    // 令牌桶的Redis键名
    private final String REDIS_KEY = "token:bucket";
    // 令牌桶最大令牌数
    private final int capacity;
    // 令牌桶填充速率（每秒填充的令牌数）
    private final double refillRate;
    // 令牌桶当前令牌数
    private double tokens = 0;
    // 上次填充令牌的时间戳
    private long lastRefillTimestamp;

    public TokenBucketLimiter(int capacity, double refillRate) {
        this.capacity = capacity;
        this.refillRate = refillRate;
        this.tokens = capacity; // 初始时桶是满??
        this.lastRefillTimestamp = System.currentTimeMillis();
    }

    public synchronized boolean tryAcquire() {
        // 1.填充令牌
        refill();
        //2.获取令牌
        if(tokens >= 1){
            // 有令牌，放行
            tokens--;
            return true;
        }else{
            // 限流
            return false;
        }
    }

    private  void refill() {
        //1.计算时间??
        long now = System.currentTimeMillis();
        double elapsed = (now - lastRefillTimestamp) / 1000.0;
        //2.计算该时间差可以生产新的令牌??
        double newTokens = elapsed * refillRate;
        //3.更新令牌??
        if(newTokens > 0){
            // 不超过最大容??
            tokens=Math.min(capacity,tokens+newTokens);
            // 更新上次填充令牌时间??
            lastRefillTimestamp = now;
        }
    }
}
