package com.umc.puppymode2.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Configuration
@EnableCaching
public class RedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.data.redis.ssl.enabled:false}")
    private boolean sslEnabled;

    @Value("${spring.data.redis.timeout:10s}")
    private Duration timeout;

    /**
     * RedisConnectionFactory 수동 설정
     * - 로컬: localhost:6379 (SSL X)
     * - 배포: AWS ElastiCache (SSL O)
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        // Redis 서버 설정
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisHost);
        config.setPort(redisPort);

        if (redisPassword != null && !redisPassword.isEmpty()) {
            config.setPassword(redisPassword);
        }

        // ClientOptions 설정
        ClientOptions.Builder clientOptionsBuilder = ClientOptions.builder()
                .socketOptions(SocketOptions.builder()
                        .connectTimeout(timeout)
                        .build());

        // SSL 설정
        if (sslEnabled) {
            // AWS ElastiCache의 자체 서명 인증서를 위해 검증 비활성화
            io.lettuce.core.SslOptions sslOptions = io.lettuce.core.SslOptions.builder()
                    .jdkSslProvider()
                    .build();

            clientOptionsBuilder.sslOptions(sslOptions);
            log.info("[REDIS SSL] Enabled with peer verification disabled (safe for AWS VPC)");
        }

        // Lettuce Client 설정
        LettuceClientConfiguration.LettuceClientConfigurationBuilder clientConfigBuilder =
                LettuceClientConfiguration.builder()
                        .commandTimeout(timeout)
                        .clientOptions(clientOptionsBuilder.build());

        if (sslEnabled) {
            clientConfigBuilder.useSsl();
        }

        LettuceClientConfiguration clientConfig = clientConfigBuilder.build();

        LettuceConnectionFactory factory = new LettuceConnectionFactory(config, clientConfig);
        factory.setValidateConnection(true);

        log.info("[REDIS CONFIG] Host={}, Port={}, SSL={}, Timeout={}",
                redisHost, redisPort, sslEnabled, timeout);

        return factory;
    }

    /**
     * RedisTemplate 설정
     * - Key: String
     * - Value: Json 직렬화
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory,
                                                       ObjectMapper objectMapper) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key 직렬화
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Value 직렬화
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer(objectMapper));
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer(objectMapper));

        template.afterPropertiesSet();
        return template;
    }

    /**
     * Redis 연결 상태를 추적하는 Bean
     */
    @Component
    @Getter
    public static class RedisHealthIndicator {
        private boolean available = false;

        public void setAvailable(boolean available) {
            this.available = available;
            if (available) {
                log.info("[REDIS HEALTH] Redis 사용 가능");
            } else {
                log.warn("[REDIS HEALTH] Redis 사용 불가 - Fallback 모드");
            }
        }
    }

    /**
     * 애플리케이션 시작 시 Redis 연결 검증
     * - 연결 실패 시 경고만 출력하고 계속 진행 (Graceful Degradation)
     */
    @Bean
    public CommandLineRunner redisConnectionCheck(
            RedisConnectionFactory cf,
            RedisHealthIndicator healthIndicator) {
        return args -> {
            try {
                String result = cf.getConnection().ping();
                log.info("[REDIS] 연결 성공! PING={}", result);
                healthIndicator.setAvailable(true);
            } catch (Exception e) {
                log.error("[REDIS] 연결 실패: {} - Redis 없이 애플리케이션 실행", e.getMessage());
                log.warn("[REDIS] Redis 관련 API는 사용 불가능합니다");
                healthIndicator.setAvailable(false);
                // 애플리케이션은 계속 실행 (예외 던지지 않음)
            }
        };
    }
}