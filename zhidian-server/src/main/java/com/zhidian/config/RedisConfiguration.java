package com.zhidian.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@Slf4j
public class RedisConfiguration {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory){
        log.info("开始创建 redis 模板对象...");
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        //设置 redis 的连接工厂对象
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        //设置 redis key 的序列化器
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        // 设置 hash key 的序列化器
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        // 设置 value 的序列化器（使用 JSON 格式）
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        // 设置 hash value 的序列化器
        redisTemplate.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        return redisTemplate;
    }
}
