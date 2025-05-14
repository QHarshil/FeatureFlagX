package com.featureflagx.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Value("${spring.redis.host:redis}") // Default to 'redis' if not specified
    private String redisHost;

    @Value("${spring.redis.port:6379}") // Default to 6379 if not specified
    private int redisPort;

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisHost, redisPort);
        // Add password if your Redis instance requires it:
        // config.setPassword(RedisPassword.of("yourpassword"));
        return new LettuceConnectionFactory(config);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory());
        template.setKeySerializer(new StringRedisSerializer());
        // For value serializer, you might want to use Jackson2JsonRedisSerializer
        // if you are storing complex objects, or StringRedisSerializer for simple strings.
        // template.setValueSerializer(new Jackson2JsonRedisSerializer<>(Object.class));
        template.setValueSerializer(new StringRedisSerializer()); // Assuming boolean flags are stored as strings or converted
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    // Specific template for boolean flags if needed, to ensure type safety
    @Bean
    public RedisTemplate<String, Boolean> booleanRedisTemplate() {
        RedisTemplate<String, Boolean> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory());
        template.setKeySerializer(new StringRedisSerializer());
        // Using a simple string serializer for boolean, Spring Data Redis handles conversion
        // Or use a custom serializer if specific format is needed
        // For simplicity, relying on default conversion or ensuring service layer handles String<->Boolean
        // template.setValueSerializer(new GenericToStringSerializer<>(Boolean.class));
        return template;
    }
}

