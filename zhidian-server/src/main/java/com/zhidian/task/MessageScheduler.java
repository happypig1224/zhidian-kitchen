package com.zhidian.task;

import com.zhidian.entity.LocalMessage;
import com.zhidian.mapper.LocalMessageMapper;
import com.zhidian.service.MessageHandlerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class MessageScheduler {
    
    @Autowired
    private LocalMessageMapper localMessageMapper;
    
    @Autowired
    private List<MessageHandlerService> messageHandlers;
    
    private Map<String, MessageHandlerService> handlerMap;

    // TODO 改用RocketMQ实现
    //@Scheduled(fixedRate = 30000) // 30s执行一次
    @Transactional
    public void processPendingMessages() {
        if (handlerMap == null) {
            handlerMap = messageHandlers.stream()
                    .collect(Collectors.toMap(MessageHandlerService::getSupportedMessageType, h -> h));
        }
        
        List<LocalMessage> pendingMessages = localMessageMapper.selectPendingMessages();
        if (pendingMessages.isEmpty()) {
            return;
        }
        log.info("开始处理待处理消息，数量: {}", pendingMessages.size());
        for (LocalMessage message : pendingMessages) {
            try {
                // 更新为处理中状态
                message.setStatus(LocalMessage.STATUS_PROCESSING);
                message.setUpdateTime(LocalDateTime.now());
                localMessageMapper.updateStatus(message);
                
                // 处理消息
                MessageHandlerService handler = handlerMap.get(message.getMessageType());
                if (handler != null) {
                    handler.handleMessage(message);
                    
                    // 处理成功
                    message.setStatus(LocalMessage.STATUS_SUCCESS);
                    message.setProcessTime(LocalDateTime.now());
                } else {
                    message.setStatus(LocalMessage.STATUS_FAILED);
                    message.setErrorMessage("找不到对应的消息处理器");
                }
                
            } catch (Exception e) {
                // 处理失败，重试逻辑
                message.setRetryCount(message.getRetryCount() + 1);
                if (message.getRetryCount() >= message.getMaxRetryCount()) {
                    message.setStatus(LocalMessage.STATUS_FAILED);
                    message.setErrorMessage("超过最大重试次数: " + e.getMessage());
                } else if(message.getRetryCount()==1){
                    // 第一次直接重置执行时间
                    message.setNextExecuteTime(LocalDateTime.now());
                }
                else {
                    message.setStatus(LocalMessage.STATUS_PENDING);
                    message.setNextExecuteTime(LocalDateTime.now().plusMinutes(5)); // 5分钟后重试
                }
            } finally {
                message.setUpdateTime(LocalDateTime.now());
                localMessageMapper.updateStatus(message);
            }
        }
    }
}
