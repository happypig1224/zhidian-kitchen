package com.zhidian.service;


import org.springframework.ai.chat.messages.Message;

import java.util.List;
import java.util.Set;

/**
* @author 33046
* @description 针对表【chat_memory(聊天记忆表)】的数据库操作 Service
* @createDate 2026-03-29 15:29:09
*/
public interface ChatMemoryService{
    void add(String conversationId, List<Message> messages);
    List<Message> get(String conversationId);
    void clear(String conversationId);
    
    /**
     * 获取所有会话 ID 列表
     * @return 会话 ID 集合
     */
    Set<String> getAllSessionIds();
}
