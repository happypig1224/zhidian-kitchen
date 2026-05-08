package com.zhidian.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

/**
 * 内存缓存工具类
 * 适用于高频访问、数据量小的场景
 */
@Component
@Slf4j
public class MemoryCacheUtil {

    // 使用ConcurrentHashMap保证线程安全
    private static final Map<String, CacheItem> cache = new ConcurrentHashMap<>();
    // 过期索引
    private static DelayQueue<ExpiredKey> expireQueue = new DelayQueue<>();
    // 定时清理过期缓存的线程池
    private static final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();

    // 本地缓存最大容量
    private static final int MAX_SIZE = 1000;

    @PostConstruct
    public void init() {
        cleanupExecutor.scheduleAtFixedRate(this::cleanupExpired, 10, 10, TimeUnit.MINUTES);
        log.info("内存缓存清理线程已启动，间隔10分钟");
    }

    @PreDestroy
    public void destroy() {
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(5L, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdown();
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdown();
            Thread.currentThread().interrupt();
        }
        log.info("内存缓存清理线程已停止");
    }

    /**
     * 缓存项
     */
    private static class CacheItem {
        private final Object value;
        private final long expireTime; // 过期时间戳
        private AtomicInteger count = new AtomicInteger(0);// 缓存访问次数

        public CacheItem(Object value, long expireTime) {
            this.value = value;
            this.expireTime = expireTime;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expireTime;
        }
        public int getCount() {
            return count.get();
        }
    }

    public static class ExpiredKey implements Delayed {
        private String key;
        private long expireTime;

        public ExpiredKey(String key, long expireTime) {
            this.key = key;
            this.expireTime = expireTime;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(expireTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed o) {
            return (int) (this.getDelay(TimeUnit.MILLISECONDS) - o.getDelay(TimeUnit.MILLISECONDS));
        }
    }

    /**
     * 设置带过期时间的缓存
     */
    public void put(String key, Object value, long timeout, TimeUnit timeUnit) {
        // 检查容量与淘汰
        if (cache.size() >= MAX_SIZE) {
            // 触发淘汰策略
            evict();
        }
        long expireTime = System.currentTimeMillis() + timeUnit.toMillis(timeout);
        cache.put(key, new CacheItem(value, expireTime));
        expireQueue.offer(new ExpiredKey(key, expireTime));
        log.debug("内存缓存设置成功 - key: {}, value: {}, timeout: {} {}", key, value, timeout, timeUnit);
    }

    /**
     * 淘汰策略
     */
    private void evict() {
        // TODO 实现LRU
        log.debug("内存缓存开始淘汰");
        // 找到访问次数最少的key
        String lfuKey = cache.entrySet().stream()
                .min(Comparator.comparingInt(e -> e.getValue().getCount()))
                .map(Map.Entry::getKey)
                .orElse(null);

        if (lfuKey != null) {
            delete(lfuKey);
            log.debug("内存缓存淘汰 - key: {}, 访问次数: {}", lfuKey, cache.get(lfuKey).count);
        }
    }

    /**
     * 获取缓存
     */
    public Object get(String key) {
        CacheItem item = cache.get(key);
        if (item != null && !item.isExpired()) {

        }
        if (item == null) {
            return null;
        }
        if (item.isExpired()) {
            cache.remove(key);
            // 删除过期索引
            expireQueue.removeIf(ex -> ex.key.equals(key));
            return null;
        }
        item.count.incrementAndGet();
        return item.value;
    }

    /**
     * 删除缓存
     */
    public void delete(String key) {
        cache.remove(key);
        // 同时删除过期索引
        expireQueue.removeIf(ek -> ek.key.equals(key));
        log.debug("内存缓存删除成功 - key: {}", key);
    }

    /**
     * 清理过期缓存
     */
    private void cleanupExpired() {
        log.info("开始清理过期缓存:{}" + System.currentTimeMillis());
        // 从DelayQueue中取出已过期的缓存并删除
        ExpiredKey expiredKey;
        while ((expiredKey = expireQueue.poll()) != null) {
            // 判断缓存是否已过期
            if (System.currentTimeMillis() > expiredKey.expireTime) {
                log.debug("内存缓存已过期 - key: {}", expiredKey.key);
                cache.remove(expiredKey.key);
            }
        }
    }

    /**
     * 缓存监控
     *
     * @return
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCount", cache.size());
        stats.put("queueSize", expireQueue.size());
        stats.put("totalAccess", cache.values().stream().mapToInt(i -> i.getCount()).sum());
        // 计算命中率等
        return stats;
    }
    public  void putIfAbsent(String key, Object value, long timeout, TimeUnit timeUnit) {
        if (!cache.containsKey(key)) {
            put(key, value, timeout, timeUnit);
        }
    }
    public int increment(String key) {
        // 获取缓存项 并增加访问次数 并返回新的访问次数
        return cache.get(key).count.incrementAndGet();
    }
    public boolean containsKey(String key) {
        return cache.containsKey(key);
    }
}