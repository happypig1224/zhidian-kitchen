package com.zhidian.service.impl;

import com.alibaba.fastjson.JSON;
import com.zhidian.entity.LocalMessage;
import com.zhidian.mapper.LocalMessageMapper;
import com.zhidian.service.LocalMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * @Author 吴汇明
 * @School 绥化学院
 * @CreateTime
 */
@Service
@Slf4j
public class LocalMessageServiceImpl implements LocalMessageService {
    @Autowired
    private LocalMessageMapper localMessageMapper;
    @Transactional
    public void sendDelayMessage(Long businessId, String messageType, Map<String, Object> content, int delayMinutes) {
        log.info("发送延迟消息，业务id：{}，消息类型：{}，延迟时间：{}分钟，消息内容：{}", businessId, messageType, delayMinutes, content);
        LocalMessage message=LocalMessage.builder()
                .businessId(businessId)
                .messageType(messageType)
                .content(JSON.toJSONString(content))
                .status(LocalMessage.STATUS_PENDING)
                .retryCount(0)
                .maxRetryCount(3)
                .nextExecuteTime(LocalDateTime.now().plusMinutes(delayMinutes))
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
        localMessageMapper.insert(message);
        log.info("发送延迟消息成功，消息id：{}", message.getId());
    }

    @Transactional
    public void sendMessage(Long businessId, String messageType, Map<String, Object> content) {
        log.info("发送即时消息，业务id：{}，消息类型：{}，消息内容：{}", businessId, messageType, content);
        LocalMessage message=LocalMessage.builder()
                .businessId(businessId)
                .messageType(messageType)
                .content(JSON.toJSONString(content))
                .status(LocalMessage.STATUS_PENDING)
                .retryCount(0)
                .maxRetryCount(0)
                .nextExecuteTime(LocalDateTime.now())
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
        localMessageMapper.insert(message);
        log.info("发送即时消息成功，消息id：{}", message.getId());
    }

    @Transactional
    public void cancelMessage(Long businessId, String messageType) {
        log.info("取消消息，业务id：{}，消息类型：{}", businessId, messageType);
        LocalMessage message=localMessageMapper.selectByBusinessIdAndType(businessId, messageType);
        if(message!=null && message.getStatus().equals(LocalMessage.STATUS_PENDING)){
            message.setStatus(LocalMessage.STATUS_FAILED);
            localMessageMapper.updateStatus(message);
            log.info("取消消息成功，消息id：{}", message.getId());
        }else{
            log.warn("未找到业务id为{}、消息类型为{}的消息", businessId, messageType);
        }
    }
}
