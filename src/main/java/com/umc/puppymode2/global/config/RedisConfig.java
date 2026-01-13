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
        // 환경변수 읽기 확인
        log.info("[REDIS ENV] REDIS_HOST={}", maskHost(redisHost));
        log.info("[REDIS ENV] REDIS_PORT={}", redisPort);
        log.info("[REDIS ENV] REDIS_PASSWORD={}", redisPassword != null && !redisPassword.isEmpty() ? "***SET***" : "***EMPTY***");
        log.info("[REDIS ENV] SSL_ENABLED={}", sslEnabled);
        log.info("[REDIS ENV] TIMEOUT={}", timeout);

        // Redis 서버 설정
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisHost);
        config.setPort(redisPort);

        if (redisPassword != null && !redisPassword.isEmpty()) {
            config.setPassword(redisPassword);
        }

        // ClientOptions 생성
        ClientOptions clientOptions = ClientOptions.builder()
                .socketOptions(SocketOptions.builder()
                        .connectTimeout(timeout)
                        .keepAlive(true)
                        .build())
                .build();

        // LettuceClientConfiguration 빌더
        LettuceClientConfiguration.LettuceClientConfigurationBuilder clientConfigBuilder =
                LettuceClientConfiguration.builder()
                        .commandTimeout(timeout)
                        .clientOptions(clientOptions);

        // SSL 활성화 (AWS ElastiCache용)
        if (sslEnabled) {
            // disablePeerVerification(): AWS ElastiCache TLS 연결에 필요
            // - Amazon Root CA가 Docker/ECS 기본 신뢰 저장소에 없을 수 있음
            // - VPC 내부 통신이므로 보안상 안전 (보안그룹으로 접근 제어, TLS 암호화 유지)
            clientConfigBuilder
                    .useSsl()
                    .disablePeerVerification();
            log.info("[REDIS SSL] TLS enabled with peer verification disabled (safe for AWS VPC)");
        }

        LettuceConnectionFactory factory =
                new LettuceConnectionFactory(config, clientConfigBuilder.build());

        factory.setValidateConnection(false);

        log.info("[REDIS CONFIG] Host={}, Port={}, SSL={}, Timeout={}",
                maskHost(redisHost), redisPort, sslEnabled, timeout);

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

    /**
     * Redis Host를 마스킹 처리
     * 예: master.redis-cache.xxx.com -> master.***
     */
    private String maskHost(String host) {
        if (host == null || host.isEmpty()) {
            return "***EMPTY***";
        }
        if (host.equals("localhost") || host.equals("127.0.0.1")) {
            return host; // 로컬은 그대로
        }
        // AWS ElastiCache 호스트 마스킹: 첫 부분만 남김
        int firstDot = host.indexOf('.');
        if (firstDot > 0) {
            return host.substring(0, firstDot) + ".***";
        }
        return "***";
    }
}