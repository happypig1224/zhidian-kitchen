package com.zhidian.controller.admin;

import com.zhidian.result.Result;
import com.zhidian.utils.MemoryCacheUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

@RestController("adminShopController")
@RequestMapping("/admin/shop")
@Tag(name = "店铺状态相关接口")
@Slf4j
public class ShopController {
    // 缓存key
    private static final String key = "SHOP_STATUS";
    @Resource
    private RedisTemplate redisTemplate;
    @Autowired
    private MemoryCacheUtil memoryCacheUtil;

    @PutMapping("/{status}")
    @Operation(summary = "店铺营业状态设置")
    public Result setStatus(@PathVariable Integer status) {
        log.info("店铺状态设置:{}", status);
        
        // 注释双写策略：同时写入Redis和内存缓存
        // 1. 写入Redis（持久化）
        redisTemplate.opsForValue().set(key, status);
        
        // 2. 写入内存缓存（高性能读取）
        memoryCacheUtil.put(key, status, 30, TimeUnit.MINUTES);
        
        log.info("店铺状态设置完成 - Redis和内存缓存已同步");

        return Result.success();
    }

    @GetMapping("/status")
    @Operation(summary = "管理端查看营业状态")
    public Result<Integer> getStatus() {
        // 注释双级缓存读取逻辑
        // 优先从内存缓存读取（性能最优）
        Integer status = (Integer) memoryCacheUtil.get(key);
        
        // 如果内存缓存不存在，从Redis读取
        if (status == null) {
            status = (Integer) redisTemplate.opsForValue().get(key);
            
            // 如果Redis存在，回写到内存缓存
            if (status != null) {
                memoryCacheUtil.put(key, status, 30, TimeUnit.MINUTES);
            }else{
                // 都不存在，默认营业中
                status = 1;
                redisTemplate.opsForValue().set(key, status);
                memoryCacheUtil.put(key, status, 30, TimeUnit.MINUTES);
            }
        }
        return Result.success(status);
    }
}