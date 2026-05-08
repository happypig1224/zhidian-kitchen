package com.zhidian.controller.user;

import com.zhidian.context.BaseContext;
import com.zhidian.entity.SeckillOrder;
import com.zhidian.entity.SeckillVoucher;
import com.zhidian.result.Result;
import com.zhidian.service.SeckillVoucherService;
import com.zhidian.utils.RedisConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author HappyPig
 * @version 1.0
 * @since 2026/3/11 17:15
 */
@RestController("userSeckillVoucherController")
@RequestMapping("/user/seckillVoucher")
@Slf4j
public class SeckillVoucherController {
    @Autowired
    private SeckillVoucherService seckillVoucherService;

    // 秒杀优惠卷
    @GetMapping("/seckill/{voucherId}")
    public Result<String> seckill(@PathVariable("voucherId") Long voucherId) {
        return seckillVoucherService.seckill(voucherId);
    }

    // 优惠卷查询
    @GetMapping("/queryVoucher")
    public Result<List<SeckillVoucher>> issueVoucher() {
        log.info("查询优惠券");
        return seckillVoucherService.queryVoucher();
    }
    @GetMapping("/myVoucherList")
    public Result<List<SeckillOrder>> myVoucherList() {
        log.info("查询用户优惠券");
        return seckillVoucherService.myVoucherList();
    }
}