package com.zhidian.config;

import com.zhidian.agent.memory.DataBaseChatMemory;
import com.zhidian.agent.tools.AddShoppingCartTool;
import com.zhidian.agent.tools.DishAndSetmealSearchTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.ToolCallAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @author HappyPig
 * @version 1.0
 * @since 2026/3/27 15:35
 */
@Configuration
public class ChatClientConfig {
    @Value("classpath:prompt/system.txt")
    private Resource systemPromptResource;
    @Autowired
    private VectorStore vectorStore;
    @Autowired
    private AddShoppingCartTool addShoppingCartTool;
    @Autowired
    private DishAndSetmealSearchTool dishAndSetmealSearchTool;

    private String loadSystemPrompt() {
        try {
            return new String(systemPromptResource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load system prompt file", e);
        }
    }

    @Bean("salesAssistantClient")
    public ChatClient chatClient(ChatModel chatModel, DataBaseChatMemory dataBaseChatMemory) {
        String systemPrompt = loadSystemPrompt();
        VectorStoreDocumentRetriever documentRetriever= VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .topK(10)
                .similarityThreshold(0.8)
                .build();
        RetrievalAugmentationAdvisor advisor= RetrievalAugmentationAdvisor.builder()
                .documentRetriever(documentRetriever)
                .build();
        return ChatClient.builder(chatModel)
                // 1. RAG 增强（最外层）
                .defaultAdvisors(advisor)
                // 2. 工具调用支持（让 AI 能够执行 Java 方法）
                .defaultTools(addShoppingCartTool, dishAndSetmealSearchTool)
                // 3. 会话记忆
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(dataBaseChatMemory)
                                .build()
                )
                // 4. 日志记录
                .defaultAdvisors(new SimpleLoggerAdvisor())
                // 设置 ChatModel Options
                .defaultOptions(ChatOptions.builder()
                        .model("qwen-plus")
                        .topK(10)
                        .topP(0.8)
                        .temperature(0.7)
                        .build())
                .defaultSystem(systemPrompt)
                .build();
    }
}
