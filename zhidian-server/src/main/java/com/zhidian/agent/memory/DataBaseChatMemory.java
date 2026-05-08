package com.zhidian.agent.memory;

import com.zhidian.context.BaseContext;
import com.zhidian.service.ChatMemoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author HappyPig
 * @version 1.0
 * @since 2026/3/29 15:26
 */
@Component
@Slf4j
public class DataBaseChatMemory implements ChatMemory {
    @Autowired
    private ChatMemoryService chatMemoryService;
    public void add(String conversationId, List<Message> messages) {
        if (messages.isEmpty()) {
            return;
        }
        log.info("添加会话记录：{}", conversationId);
        chatMemoryService.add(conversationId, messages);
    }

    public List<Message> get(String conversationId) {
        log.info("获取会话记录：{}", conversationId);
        return chatMemoryService.get(conversationId);
    }

    public void clear(String conversationId) {
        log.info("清空会话记录：{}", conversationId);
        chatMemoryService.clear(conversationId);
    }
}
