package com.zhidian.controller.admin;

import com.zhidian.entity.SeckillVoucher;
import com.zhidian.result.Result;
import com.zhidian.service.SeckillVoucherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author HappyPig
 * @version 1.0
 * @since 2026/3/11 17:15
 */
@RestController("adminSeckillVoucherController")
@RequestMapping("/admin/seckillVoucher")
public class SeckillVoucherController {
    @Autowired
    private SeckillVoucherService seckillVoucherService;
    /**
     * 发送秒杀券
     * @param seckillVoucher
     * @return
     */
    @PostMapping("/publish")
    public Result<String> publishVoucher(@RequestBody SeckillVoucher seckillVoucher) {
        System.out.println("发送秒杀券" + seckillVoucher);
        return seckillVoucherService.issueVoucher(seckillVoucher);
    }
}
