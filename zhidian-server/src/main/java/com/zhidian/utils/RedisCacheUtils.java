package com.zhidian.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.Random;

/**
 * Redis 缓存工具类
 * 封装了解决缓存穿透、击穿、雪崩的通用方法
 */
public class RedisCacheUtils {

    private static final StringRedisTemplate stringRedisTemplate = new StringRedisTemplate();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    

    // 随机数生成器，用于解决雪崩
    private static final Random RANDOM = new Random();

    // 缓存空值占位符
    private static final String CACHE_NULL_FLAG = "__NULL__";

    /**
     * 1. 解决缓存穿透
     * 策略：缓存空对象。如果数据库也没数据，写入一个空值占位符，并设置较短过期时间。
     *
     * @param key          缓存键
     * @param type         返回数据类型
     * @param dbFunction   数据库查询函数 (例如: id -> userMapper.selectById(id))
     * @param expireTime   正常数据的过期时间
     * @param timeUnit     时间单位
     * @param <T>          泛型
     * @return 查询结果
     */
    public static <T> T queryWithPassThrough(String key, Class<T> type, Function<String, T> dbFunction, long expireTime, TimeUnit timeUnit) {
        // 1. 查询缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        
        // 2. 缓存命中
        if (StringUtils.hasText(json)) {
            // 如果是空值占位符，说明数据库也没数据，直接返回 null
            if (CACHE_NULL_FLAG.equals(json)) {
                return null;
            }
            try {
                return objectMapper.readValue(json, type);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }

        // 3. 缓存未命中，查询数据库
        // 注意：这里没有加锁，因为穿透通常是针对不存在的ID，并发量相对击穿较小，
        // 或者可以在业务层做参数校验（如ID <= 0 直接返回）作为第一道防线。
        T data = dbFunction.apply(key);

        // 4. 写入缓存
        if (data == null) {
            // 解决穿透的核心：将空值也写入缓存，设置较短过期时间（如5分钟）
            stringRedisTemplate.opsForValue().set(key, CACHE_NULL_FLAG, 5, TimeUnit.MINUTES);
            return null;
        }

        // 写入正常数据
        try {
            stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(data), expireTime, timeUnit);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return data;
    }

    /**
     * 2. 解决缓存击穿
     * 策略：互斥锁。缓存失效时，只允许一个线程重建缓存，其他线程等待。
     *
     * @param key          缓存键
     * @param type         返回数据类型
     * @param dbFunction   数据库查询函数
     * @param expireTime   过期时间
     * @param timeUnit     时间单位
     * @param <T>          泛型
     * @return 查询结果
     */
    public static <T> T queryWithMutex(String key, Class<T> type, Function<String, T> dbFunction, long expireTime, TimeUnit timeUnit) {
        // 1. 查询缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        if (StringUtils.hasText(json)) {
            if (CACHE_NULL_FLAG.equals(json)) return null;
            try {
                return objectMapper.readValue(json, type);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }

        // 2. 缓存未命中，尝试获取互斥锁
        T data;
        String lockKey = "lock:" + key;
        boolean isLock = tryLock(lockKey);
        
        try {
            if (isLock) {
                // 3.1 获取锁成功，再次检测缓存（双重检查）
                json = stringRedisTemplate.opsForValue().get(key);
                if (StringUtils.hasText(json)) {
                     // 其他线程已经重建了缓存
                     if (CACHE_NULL_FLAG.equals(json)) return null;
                     try { return objectMapper.readValue(json, type); } catch (JsonProcessingException e) { e.printStackTrace(); }
                }

                // 3.2 查询数据库
                data = dbFunction.apply(key);
                
                // 3.3 写入缓存（处理空值防止穿透）
                if (data == null) {
                    stringRedisTemplate.opsForValue().set(key, CACHE_NULL_FLAG, 5, TimeUnit.MINUTES);
                } else {
                    try {
                        stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(data), expireTime, timeUnit);
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                // 3.4 获取锁失败，休眠重试
                Thread.sleep(50);
                // 递归调用自己，重新尝试读取缓存
                return queryWithMutex(key, type, dbFunction, expireTime, timeUnit);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } finally {
            // 4. 释放锁
            if (isLock) {
                unlock(lockKey);
            }
        }
        return data;
    }

    /**
     * 3. 解决缓存雪崩
     * 策略：随机过期时间。在原有过期时间基础上增加随机值，避免集体失效。
     *
     * @param key          缓存键
     * @param value        缓存值
     * @param baseExpire   基础过期时间
     * @param randomBound  随机波动范围（例如 300 表示 +/- 300秒）
     * @param timeUnit     时间单位
     */
    public static void setWithRandomExpire(String key, Object value, long baseExpire, long randomBound, TimeUnit timeUnit) {
        try {
            // 计算随机时间：base + random(-bound ~ +bound)
            // 确保最终时间不为负数
            long randomOffset = randomBound > 0 ? RANDOM.nextInt((int) (randomBound * 2)) - randomBound : 0;
            long finalExpire = baseExpire + randomOffset;
            if (finalExpire <= 0) finalExpire = baseExpire; // 兜底

            String json = objectMapper.writeValueAsString(value);
            stringRedisTemplate.opsForValue().set(key, json, finalExpire, timeUnit);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    // --- 辅助方法：简单的分布式锁实现 ---

    private static boolean tryLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(flag);
    }

    private static void unlock(String key) {
        stringRedisTemplate.delete(key);
    }
}