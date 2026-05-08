package com.zhidian.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Date;

/**
 * @author HappyPig
 * @version 1.0
 * @since 2026/3/23 23:23
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class SeckillMessage  {
    private Long userId;
    private Long voucherId;
}
