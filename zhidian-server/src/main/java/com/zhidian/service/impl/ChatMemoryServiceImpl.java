package com.zhidian.service.impl;

import com.zhidian.context.BaseContext;
import com.zhidian.entity.ChatMemory;
import com.zhidian.mapper.ChatMemoryMapper;
import com.zhidian.service.ChatMemoryService;
import org.springframework.ai.chat.messages.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author 33046
 * @description 针对表【chat_memory(聊天记忆表)】的数据库操作Service实现
 * @createDate 2026-03-29 15:29:09
 */
@Service
public class ChatMemoryServiceImpl implements ChatMemoryService {

    @Autowired
    private ChatMemoryMapper chatMemoryMapper;

    @Override
    public void add(String conversationId, List<Message> messages) {
        messages.stream().forEach(message -> {
                MessageType messageType = message.getMessageType();
                ChatMemory chatMemory = null;
                switch (messageType) {
                    case USER:
                        chatMemory = ChatMemory.builder()
                                .sessionId(conversationId)
                                .messageType(messageType.name())
                                .content(message.getText())
                                .build();
                        break;
                    case SYSTEM:
                        chatMemory = ChatMemory.builder()
                                .sessionId(conversationId)
                                .messageType(messageType.name())
                                .content(message.getText())
                                .build();
                        break;
                    case ASSISTANT:
                        chatMemory = ChatMemory.builder()
                                .sessionId(conversationId)
                                .messageType(messageType.name())
                                .content(message.getText())
                                .build();
                        break;
                }
                
                if (chatMemory != null) {
                    chatMemory.setCreateTime(new java.sql.Timestamp(System.currentTimeMillis()));
                    chatMemory.setUpdateTime(new java.sql.Timestamp(System.currentTimeMillis()));
                    chatMemory.setIsDeleted(0);
                    chatMemoryMapper.insert(chatMemory);
                }
        });
    }

    @Override
    public List<Message> get(String conversationId) {
        List<ChatMemory> chatMemoryList = chatMemoryMapper.selectBySessionId(conversationId);
        if (chatMemoryList.isEmpty()) {
            return List.of();
        }
        return chatMemoryList.stream().map(chatMemory -> {
            switch (chatMemory.getMessageType()) {
                case "USER":
                    return UserMessage.builder().text(chatMemory.getContent()).build();
                case "SYSTEM":
                    return SystemMessage.builder().text(chatMemory.getContent()).build();
                case "ASSISTANT":
                    return AssistantMessage.builder().content(chatMemory.getContent()).build();
            }
            return null;
        }).collect(Collectors.toList());
    }

    @Override
    public void clear(String conversationId) {
        chatMemoryMapper.deleteBySessionId(conversationId);
    }

    @Override
    public Set<String> getAllSessionIds() {
        return chatMemoryMapper.selectAllSessionIds();
    }
}