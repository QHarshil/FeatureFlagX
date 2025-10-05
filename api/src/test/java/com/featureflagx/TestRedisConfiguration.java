package com.featureflagx;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@TestConfiguration
public class TestRedisConfiguration {

    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> template = Mockito.mock(RedisTemplate.class);
        ValueOperations<String, Object> valueOps = Mockito.mock(ValueOperations.class);
        Mockito.when(template.opsForValue()).thenReturn(valueOps);
        return template;
    }
}
