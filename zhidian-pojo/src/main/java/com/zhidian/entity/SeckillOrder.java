package com.zhidian.entity;

import java.time.LocalDateTime;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 秒杀订单表
 * @TableName seckill_order
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeckillOrder {
    /**
     * 主键
     */
    private Long id;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 优惠券id
     */
    private Long voucherId;

    /**
     * 下单时间
     */
    private LocalDateTime orderTime;

    /**
     * 1,待支付; 2,已支付; 3,已取消; 4,已退款; 5,支付失败; 6,已删除
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}