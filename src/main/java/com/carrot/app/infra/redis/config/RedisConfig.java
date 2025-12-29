package com.carrot.app.infra.redis.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.carrot.app.domain.chat.service.RedisSubscriber;
import com.carrot.app.global.common.CacheKey;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.context.annotation.Primary;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Configuration
@EnableCaching
@EnableAsync
public class RedisConfig {
        // 일반적인 API 통신을 위한 기본 ObjectMapper (DefaultTyping 없음)
        @Bean
        @Primary
        public ObjectMapper objectMapper() {
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.registerModule(new JavaTimeModule());
                return objectMapper;
        }

        // 자바 객체를 json 형태로 변환하는 객체
        // 자바 객체를 json으로 변환할 때 클래스를 함께 넣어주고 있음
        // msa로 변경 시 수정 필요
        // Redis 전용 ObjectMapper (객체 타입 보존을 위해 @class 정보 포함)
        @Bean(name = "redisObjectMapper")
        public ObjectMapper redisObjectMapper() {
                PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                                .allowIfBaseType(Object.class)
                                .build();

                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.registerModule(new JavaTimeModule());
                objectMapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);

                return objectMapper;
        }

        // 스프링에서 Redis에 명령어를 실행할 수 있도록 하는 객체
        @Bean
        public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory,
                        @Qualifier("redisObjectMapper") ObjectMapper redisObjectMapper) {
                RedisTemplate<String, Object> template = new RedisTemplate<>();
                template.setConnectionFactory(connectionFactory);

                GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(
                                redisObjectMapper);

                // Key is String
                template.setKeySerializer(new StringRedisSerializer());

                // Value is JSON
                template.setValueSerializer(jsonSerializer);

                // Hash
                template.setHashKeySerializer(new StringRedisSerializer());
                template.setHashValueSerializer(jsonSerializer);

                template.afterPropertiesSet();
                return template;
        }

        // @Cacheable 어노테이션이 붙은 메서드의 결과를 자동으로 Redis에 저장하고 관리하는 메니저
        @Bean
        public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory,
                        @Qualifier("redisObjectMapper") ObjectMapper redisObjectMapper) {
                GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(
                                redisObjectMapper);

                // 기본 설정 (키, 값 직렬화, 기본 ttl 지정)
                RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                                .serializeKeysWith(
                                                RedisSerializationContext.SerializationPair
                                                                .fromSerializer(new StringRedisSerializer()))
                                .serializeValuesWith(RedisSerializationContext.SerializationPair
                                                .fromSerializer(jsonSerializer))
                                .entryTtl(Duration.ofMinutes(10)); // Default TTL

                // 캐시별 설정
                Map<String, RedisCacheConfiguration> cacheConfigMap = new HashMap<>();

                cacheConfigMap.put(CacheKey.PRODUCTS,
                                defaultConfig.entryTtl(Duration.ofSeconds(CacheKey.PRODUCTS_TTL)));
                cacheConfigMap.put(CacheKey.PRODUCT_DETAIL,
                                defaultConfig.entryTtl(Duration.ofSeconds(CacheKey.PRODUCT_DETAIL_TTL)));
                cacheConfigMap.put(CacheKey.CATEGORIES,
                                defaultConfig.entryTtl(Duration.ofSeconds(CacheKey.CATEGORIES_TTL)));
                cacheConfigMap.put(CacheKey.CHAT_ROOMS,
                                defaultConfig.entryTtl(Duration.ofSeconds(CacheKey.CHAT_ROOMS_TTL)));
                cacheConfigMap.put(CacheKey.POPULAR_KEYWORDS,
                                defaultConfig.entryTtl(Duration.ofSeconds(CacheKey.POPULAR_KEYWORDS_TTL)));

                return RedisCacheManager.builder(connectionFactory)
                                .cacheDefaults(defaultConfig)
                                .withInitialCacheConfigurations(cacheConfigMap)
                                .build();
        }

        // Redis Pub/Sub 메시지 리스너 컨테이너
        @Bean
        public RedisMessageListenerContainer redisMessageListener(
                        RedisConnectionFactory connectionFactory,
                        MessageListenerAdapter listenerAdapter) {
                RedisMessageListenerContainer container = new RedisMessageListenerContainer();

                container.addMessageListener(listenerAdapter, new PatternTopic("chat:room:*"));
                container.setConnectionFactory(connectionFactory);
                return container;
        }

        @Bean
        public MessageListenerAdapter listenerAdapter(RedisSubscriber subscriber) {
                // RedisSubscriber를 리스너로 등록
                return new MessageListenerAdapter(subscriber);
        }
}
