//package com.umc.puppymode2.global.config;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.umc.puppymode2.global.apiPayload.code.status.ErrorStatus;
//import com.umc.puppymode2.global.exception.GeneralException;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.cache.annotation.EnableCaching;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.data.redis.connection.RedisConnectionFactory;
//import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
//import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
//import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
//import org.springframework.data.redis.serializer.StringRedisSerializer;
//
//import java.time.Duration;
//
//@Slf4j
//@Configuration
//@EnableCaching
//public class RedisConfig {
//
//    @Value("${spring.data.redis.host}")
//    private String host;
//
//    @Value("${spring.data.redis.port}")
//    private int port;
//
//    @Value("${spring.data.redis.password}")
//    private String password;
//
//    @Value("${spring.data.redis.timeout}")
//    private Duration timeout;
//
//    @Value("${spring.data.redis.ssl:false}")
//    private boolean sslEnabled;
//
//    /**
//     * LettuceClientFactory 생성
//     * - ssl 사용
//     * - password 설정
//     * - connection timeout 지정
//     *
//     * @return
//     */
//    @Bean
//    public RedisConnectionFactory redisConnectionFactory() {
//        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
//        config.setHostName(host);
//        config.setPort(port);
//
//        if (password != null && !password.isBlank()) {
//            config.setPassword(password);
//        }
//
//        LettuceClientConfiguration.LettuceClientConfigurationBuilder builder =
//                LettuceClientConfiguration.builder()
//                        .commandTimeout(timeout)
//                        .shutdownTimeout(Duration.ofMillis(100));
//
//        // 환경에 따라 TLS 적용
//        if (sslEnabled) {
//            builder.useSsl();
//        }
//
//        LettuceClientConfiguration clientConfiguration = builder.build();
//
//        return new LettuceConnectionFactory(config, clientConfiguration);
//    }
//
//    /**
//     * RedisTemplate 설정
//     * - Key: String
//     * - Value: Json 직렬화
//     * - HashKey: String
//     * - HashValue: Json 직렬화
//     *
//     * @param connectionFactory
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
//    public CommandLineRunner safeRedisCheck(LettuceConnectionFactory cf) {
//        return args -> {
//            try {
//                log.info("[REDIS] {}", cf.getConnection().ping());
//            } catch (Exception e) {
//                log.warn("[REDIS] 초기 연결 실패 {}", e.getMessage());
//                throw new IllegalStateException("Redis 연결 실패 - 애플리케이션 중단", e);
//            }
//        };
//    }
//}
