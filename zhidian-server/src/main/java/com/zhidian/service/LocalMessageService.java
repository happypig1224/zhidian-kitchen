package com.zhidian.service;

import java.util.Map;

/**
 * @Author 吴汇明
 * @School 绥化学院
 * @CreateTime
 */
public interface LocalMessageService {
    /**
     * 发送延迟消息
     * @param businessId 业务id
     * @param messageType 消息类型
     * @param content 消息内容
     * @param delayMinutes 延迟时间，单位：分钟
     */
    void sendDelayMessage(Long businessId, String messageType, Map<String,Object> content,int delayMinutes);
    /**
     * 发送即时消息
     * @param businessId 业务id
     * @param messageType 消息类型
     * @param content 消息内容
     */
    void sendMessage(Long businessId, String messageType, Map<String,Object> content);
    /**
     * 取消消息
     * @param businessId 业务id
     * @param messageType 消息类型
     */
    void cancelMessage(Long businessId,String messageType);
}
