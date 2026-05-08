package com.zhidian.config;

import org.redisson.config.Config;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.DefaultJedisClientConfig;

/**
 * @author HappyPig
 * @version 1.0
 * @since 2026/3/29 23:43
 */
@Configuration
public class VectorStoreConfig {
    JedisPooled jedisPooled;

    public VectorStoreConfig() {
        HostAndPort hostAndPort = new HostAndPort("106.55.22.153", 6379);
        JedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
                .password("123456")
                .build();
        jedisPooled = new JedisPooled(hostAndPort, clientConfig);
    }

    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return RedisVectorStore.builder(jedisPooled, embeddingModel)
                .prefix("vector-redis")
                .initializeSchema(true)
                .indexName("zhiwei-kitchen-index")
                .build();
    }
}
