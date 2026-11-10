package com.innowise.userservice.config;

import com.innowise.userservice.dto.UserResponseDto;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableCaching
public class CacheConfig {
    @Bean
    RedisCacheManagerBuilderCustomizer userCache(ObjectMapper mapper,
            @Value("${spring.cache.redis.time-to-live}") Duration ttl) {
        var serializer = new JacksonJsonRedisSerializer<>(mapper, UserResponseDto.class);
        var config = RedisCacheConfiguration.defaultCacheConfig()
                .computePrefixWith(name -> "user-service:json:v1:" + name + "::")
                .entryTtl(ttl)
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));
        return builder -> builder.transactionAware().withCacheConfiguration("users", config);
    }
}
