package com.zhidian.service;

import com.zhidian.entity.LocalMessage;

/**
 * @Author 吴汇明
 * @School 绥化学院
 * @CreateTime
 */
public interface MessageHandlerService {
    void handleMessage(LocalMessage message);
    String getSupportedMessageType();
}
