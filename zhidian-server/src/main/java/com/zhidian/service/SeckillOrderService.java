package com.zhidian.service;

import com.zhidian.dto.SeckillMessage;
import com.zhidian.entity.SeckillOrder;

/**
* @author 33046
* @description 针对表【seckill_order(秒杀订单表)】的数据库操作Service
* @createDate 2026-03-11 16:54:18
*/
public interface SeckillOrderService {
    int createOrder(SeckillMessage seckillMessage);
}
