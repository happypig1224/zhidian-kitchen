package com.zhidian.controller.user;

import com.zhidian.agent.memory.DataBaseChatMemory;
import com.zhidian.context.BaseContext;
import com.zhidian.result.Result;
import com.zhidian.service.ChatMemoryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AI导购模块
 *
 * @author HappyPig
 * @version 1.0
 * @since 2026/3/27 12:04
 */
@RestController
@Tag(name = "C 端 AI 导购接口")
@RequestMapping("/user/ai")
@Slf4j
public class AISalesAssistantController {
    @Autowired
    @Qualifier("salesAssistantClient")
    private ChatClient salesAssistantClient;

    @Autowired
    private ChatMemoryService chatMemoryService;

    // 对话接口，携带 sessionId，自动携带历史消息
    @GetMapping(value = "/chat", produces = "application/json;charset=utf-8")
    public Flux<String> chat(@RequestParam("sessionId") String sessionId,
                             @RequestParam("question") String question) {
        log.info("sessionId:{},question:{}", sessionId, question);
        // 获取当前登录用户 ID，实现用户级别的会话隔离
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }

        // 组合用户 ID 和会话 ID，确保不同用户之间的会话完全隔离
        String conversationId = userId + ":" + sessionId;


        // 构建带历史消息的 prompt，通过 advisor 自动管理会话历史
        return salesAssistantClient.prompt()
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, conversationId)) // 使用组合 ID 传递
                .toolContext(Map.of("userId", userId))
                .user(question)
                .stream()
                .content();
    }

    // 删除会话接口，携带 sessionId
    @DeleteMapping("/delete")
    public Result<String> deleteSession(@RequestParam("sessionId") String sessionId) {
        log.info("删除会话：{}", sessionId);
        try {
            // 获取当前登录用户 ID，确保用户只能删除自己的会话
            Long userId = BaseContext.getCurrentId();
            if (userId == null) {
                return Result.error("用户未登录");
            }

            // 组合用户 ID 和会话 ID
            String conversationId = userId + ":" + sessionId;
            chatMemoryService.clear(conversationId);
            return Result.success("删除成功");
        } catch (Exception e) {
            log.error("删除会话失败：{}", e.getMessage());
            return Result.error("删除失败：" + e.getMessage());
        }
    }

    // 获取历史会话记录（当前用户所有 session）
    @GetMapping("/history")
    public Result<Set<String>> getHistory() {
        log.info("获取历史会话记录");
        try {
            // 获取当前登录用户 ID
            Long userId = BaseContext.getCurrentId();
            if (userId == null) {
                return Result.error("用户未登录");
            }
            // 获取所有会话 ID，并过滤出当前用户的会话
            Set<String> allSessionIds = chatMemoryService.getAllSessionIds();
            Set<String> userSessionIds = allSessionIds.stream()
                    .filter(sessionId -> sessionId.startsWith(userId + ":"))
                    .map(sessionId -> sessionId.substring((userId + ":").length()))
                    .collect(Collectors.toSet());

            return Result.success(userSessionIds);
        } catch (Exception e) {
            log.error("获取历史会话记录失败：{}", e.getMessage());
            return Result.error("获取失败：" + e.getMessage());
        }
    }

    // 获取某次会话的所有消息记录
    @GetMapping("/session")
    public Map<String, Object> getSession(@RequestParam("sessionId") String sessionId) {
        log.info("获取会话记录：{}", sessionId);
        Map<String, Object> result = new HashMap<>();
        try {
            // 获取当前登录用户 ID，确保用户只能查看自己的会话
            Long userId = BaseContext.getCurrentId();
            if (userId == null) {
                result.put("code", 401);
                result.put("message", "用户未登录");
                return result;
            }

            // 组合用户 ID 和会话 ID
            String conversationId = userId + ":" + sessionId;
            List<Message> messages = chatMemoryService.get(conversationId);
            List<Map<String, String>> messageList = messages.stream()
                    .map(message -> {
                        Map<String, String> msg = new HashMap<>();
                        msg.put("role", message.getMessageType().name());
                        msg.put("content", message.getText());
                        return msg;
                    })
                    .collect(Collectors.toList());

            result.put("code", 200);
            result.put("message", "获取成功");
            result.put("data", messageList);
        } catch (Exception e) {
            log.error("获取会话记录失败：{}", e.getMessage());
            result.put("code", 500);
            result.put("message", "获取失败：" + e.getMessage());
        }
        return result;
    }
}
