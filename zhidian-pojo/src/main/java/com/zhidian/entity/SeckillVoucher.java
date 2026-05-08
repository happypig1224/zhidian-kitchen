package com.zhidian.entity;

import java.time.LocalDateTime;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * 秒杀优惠券表
 * @TableName seckill_voucher
 */
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeckillVoucher {
    /**
     * 主键
     */
    private Long id;

    /**
     * 优惠券id
     */
    private Long voucherId;

    /**
     * 优惠卷标题
     */
    private String title;

    /**
     * 使用规则
     */
    private String rules;

    /**
     * 支付金额，单位是分。例如200代表2元
     */
    private Long payValue;

    /**
     * 抵扣金额，单位是分。例如200代表2元
     */
    private Long actualValue;

    /**
     * 1,上架; 2,下架; 3,过期
     */
    private Integer status;

    /**
     * 库存
     */
    private Integer stock;

    /**
     * 生效时间
     */
    private LocalDateTime beginTime;

    /**
     * 失效时间
     */
    private LocalDateTime endTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}