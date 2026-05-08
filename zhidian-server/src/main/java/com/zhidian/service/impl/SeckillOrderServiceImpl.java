package com.zhidian.service.impl;

import com.zhidian.dto.SeckillMessage;
import com.zhidian.entity.SeckillOrder;
import com.zhidian.service.SeckillOrderService;
import com.zhidian.mapper.SeckillOrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
* @author 33046
* @description 针对表【seckill_order(秒杀订单表)】的数据库操作Service实现
* @createDate 2026-03-11 16:54:18
*/
@Service
public class SeckillOrderServiceImpl implements SeckillOrderService {

    @Autowired
    private SeckillOrderMapper seckillOrderMapper;
    @Override
    public int createOrder(SeckillMessage seckillMessage) {
        SeckillOrder seckillOrder = SeckillOrder.builder()
                .userId(seckillMessage.getUserId())
                .voucherId(seckillMessage.getVoucherId())
                .orderTime(LocalDateTime.now())
                .status(1)
                .createTime(LocalDateTime.now())
                .build();
        return seckillOrderMapper.save(seckillOrder);
    }
}




