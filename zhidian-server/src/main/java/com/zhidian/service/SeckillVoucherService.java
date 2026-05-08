package com.zhidian.service;

import com.zhidian.entity.SeckillOrder;
import com.zhidian.entity.SeckillVoucher;
import com.zhidian.result.Result;

import java.util.List;

/**
* @author 33046
* @description 针对表【seckill_voucher(秒杀优惠券表)】的数据库操作Service
* @createDate 2026-03-11 16:54:18
*/
public interface SeckillVoucherService {
    // 秒杀优惠卷签发
    Result<String> issueVoucher(SeckillVoucher seckillVoucher);

    Result<String> seckill(Long voucherId);

    Result<List<SeckillVoucher>> queryVoucher();

    Result<List<SeckillOrder>> myVoucherList();

}
