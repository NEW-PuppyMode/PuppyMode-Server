package com.umc.puppymode2.global.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
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

    // TCP 연결 자체를 맺는 데 걸리는 시간 제한. 연결 수립 단계라 다소 여유 있게 잡아도 된다.
    @Value("${spring.data.redis.connect-timeout:10s}")
    private Duration connectTimeout;

    // 명령(GET/SET/DEL 등) 하나의 응답을 기다리는 시간 제한.
    // Spring Boot 컨벤션대로 spring.data.redis.timeout을 그대로 명령 타임아웃에 쓴다.
    // 이 프로퍼티를 새 이름으로 바꿔버리면, 배포 환경이 이미 spring.data.redis.timeout을
    // 설정해뒀을 때 그 값이 조용히 무시되고 다른 의미(연결 타임아웃)로 바뀌어버린다.
    // connectTimeout과 값을 공유하던 예전 방식은, Redis가 연결은 됐지만 응답이 없는
    // 상태(네트워크 지연, GC 정지 등)일 때 캐시 조회 한 번이 수십 초간 요청 스레드를
    // 붙잡아 서비스 전체 가용성을 떨어뜨릴 수 있어 분리했다.
    @Value("${spring.data.redis.timeout:10s}")
    private Duration commandTimeout;

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
        log.info("[REDIS ENV] CONNECT_TIMEOUT={}", connectTimeout);
        log.info("[REDIS ENV] COMMAND_TIMEOUT={}", commandTimeout);

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
                        .connectTimeout(connectTimeout)
                        .keepAlive(true)
                        .build())
                .build();

        // LettuceClientConfiguration 빌더
        LettuceClientConfiguration.LettuceClientConfigurationBuilder clientConfigBuilder =
                LettuceClientConfiguration.builder()
                        .commandTimeout(commandTimeout)
                        .clientOptions(clientOptions);

        // SSL 활성화 (AWS ElastiCache용)
        if (sslEnabled) {
            clientConfigBuilder
                    .useSsl();
            log.info("[REDIS SSL] TLS enabled");
        }

        LettuceConnectionFactory factory =
                new LettuceConnectionFactory(config, clientConfigBuilder.build());

        factory.setValidateConnection(false);

        log.info("[REDIS CONFIG] Host={}, Port={}, SSL={}, ConnectTimeout={}, CommandTimeout={}",
                maskHost(redisHost), redisPort, sslEnabled, connectTimeout, commandTimeout);

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
        // API 응답용 objectMapper를 그대로 넘기면 캐시된 JSON에 타입 정보(@class)가 남지 않아,
        // 조회 시 원래 DTO가 아닌 LinkedHashMap으로 역직렬화되어 캐시가 항상 미스로 처리되는 문제가 있었다.
        // Redis 전용 복사본에만 다형적 타입 정보를 활성화해서 이 문제를 해결한다.
        // (원본 objectMapper를 직접 수정하면 REST API 응답 JSON에도 @class가 섞여 나가게 되므로 복사본을 사용)
        // allowIfSubType으로 우리 도메인 패키지만 역직렬화 대상으로 허용해,
        // 캐시된 값이 조작되더라도 임의 클래스가 역직렬화되는 걸 막는다 (Jackson 다형적 역직렬화 취약점 대비).
        ObjectMapper redisObjectMapper = objectMapper.copy();
        PolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.umc.puppymode2")
                .build();
        redisObjectMapper.activateDefaultTyping(
                typeValidator,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        GenericJackson2JsonRedisSerializer valueSerializer = new GenericJackson2JsonRedisSerializer(redisObjectMapper);
        template.setValueSerializer(valueSerializer);
        template.setHashValueSerializer(valueSerializer);

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