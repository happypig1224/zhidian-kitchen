package com.zhidian.controller.user;

import com.zhidian.result.Result;
import com.zhidian.utils.MemoryCacheUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

@RestController("userShopController")
@RequestMapping("/user/shop")
@Tag(name = "店铺状态相关接口")
@Slf4j
public class ShopController {
    // 内存缓存key
    private static final String key = "SHOP_STATUS";
    @Autowired
    private MemoryCacheUtil memoryCacheUtil;
    @Autowired
    private RedisTemplate redisTemplate;

    @GetMapping("/status")
    @Operation(summary = "用户端查看营业状态")
    public Result<Integer> getStatus() {
        // 注释内存缓存读取逻辑
        // 内存缓存读取逻辑
        Integer status = (Integer) memoryCacheUtil.get(key);
        // 默认返回营业状态
        if (status == null) {
            status = (Integer) redisTemplate.opsForValue().get(key);
            if (status == null) {
                status = 1;
                memoryCacheUtil.put(key, status, 30, TimeUnit.MINUTES);
                redisTemplate.opsForValue().set(key, status);
            }
        }
        log.debug("用户端获取店铺状态: {}", status == 1 ? "营业中" : "打烊中");
        return Result.success(status);

    }
}