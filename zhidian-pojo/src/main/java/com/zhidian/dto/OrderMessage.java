package com.zhidian.dto;

import lombok.Builder;

/**
 * @author HappyPig
 * @version 1.0
 * @since 2026/3/25 11:57
 */
@lombok.Data
@lombok.AllArgsConstructor
@lombok.NoArgsConstructor
@Builder
public class OrderMessage {
    private Long orderId;
    private Long userId;
}
