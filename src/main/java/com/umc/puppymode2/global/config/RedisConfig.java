//package com.umc.puppymode2.global.config;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.cache.annotation.EnableCaching;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.data.redis.connection.RedisConnectionFactory;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
//import org.springframework.data.redis.serializer.StringRedisSerializer;
//
//@Slf4j
//@Configuration
//@EnableCaching
//public class RedisConfig {
//
//    /**
//     * RedisTemplate 설정
//     * - Key: String
//     * - Value: Json 직렬화
//     * - HashKey: String
//     * - HashValue: Json 직렬화
//     *
//     * @param connectionFactory Spring Boot가 자동 생성한 ConnectionFactory
//     * @param objectMapper
//     * @return
//     */
//    @Bean
//    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory,
//                                                       ObjectMapper objectMapper) {
//        RedisTemplate<String, Object> template = new RedisTemplate<>();
//        template.setConnectionFactory(connectionFactory);
//
//        // Key 직렬화
//        template.setKeySerializer(new StringRedisSerializer());
//        template.setHashKeySerializer(new StringRedisSerializer());
//
//        // Value 직렬화
//        template.setValueSerializer(new GenericJackson2JsonRedisSerializer(objectMapper));
//        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer(objectMapper));
//
//        template.afterPropertiesSet();
//        return template;
//    }
//
//    @Bean
//    public CommandLineRunner safeRedisCheck(RedisConnectionFactory cf) {
//        return args -> {
//            try {
//                String result = cf.getConnection().ping();
//                log.info("[REDIS] 연결 성공! - PING 응답: {}", result);
//            } catch (Exception e) {
//                log.warn("[REDIS] 초기 연결 실패: {}", e.getMessage());
//                throw new IllegalStateException("Redis 연결 실패 - 애플리케이션 중단", e);
//            }
//        };
//    }
//}