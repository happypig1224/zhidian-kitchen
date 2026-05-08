package com.zhidian.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 本地消息表实体
 * 用于替代RocketMQ，实现异步消息处理
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocalMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 消息状态常量
     */
    public static final String STATUS_PENDING = "PENDING";//待处理
    public static final String STATUS_PROCESSING = "PROCESSING";//处理中
    public static final String STATUS_SUCCESS = "SUCCESS";//成功
    public static final String STATUS_FAILED = "FAILED";//失败

    /**
     * 消息类型常量
     */
    public static final String TYPE_ORDER_TIMEOUT = "ORDER_TIMEOUT";//订单超时
    public static final String TYPE_ORDER_STATUS_CHANGE = "ORDER_STATUS_CHANGE";//订单状态变更
    public static final String TYPE_SHOPPING_CART_UPDATE = "SHOPPING_CART_UPDATE";//购物车更新

    /**
     * 主键ID
     */
    private Long id;
    
    /**
     * 业务ID（如订单ID）
     */
    private Long businessId;
    
    /**
     * 消息类型
     */
    private String messageType;
    
    /**
     * 消息内容（JSON格式）
     */
    private String content;
    
    /**
     * 消息状态
     */
    private String status;
    
    /**
     * 重试次数
     */
    private Integer retryCount;
    
    /**
     * 最大重试次数
     */
    private Integer maxRetryCount;
    
    /**
     * 下次执行时间
     */
    private LocalDateTime nextExecuteTime;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    
    /**
     * 处理完成时间
     */
    private LocalDateTime processTime;
    
    /**
     * 错误信息
     */
    private String errorMessage;
}
