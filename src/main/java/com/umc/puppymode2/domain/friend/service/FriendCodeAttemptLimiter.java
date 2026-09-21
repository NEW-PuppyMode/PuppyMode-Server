package com.umc.puppymode2.domain.friend.service;

import com.umc.puppymode2.domain.friend.exception.FriendErrorStatus;
import com.umc.puppymode2.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 친구 코드 입력 실패 횟수 제한 (코드 무작위 대입 방지).
 *
 * 친구 코드는 4자리 숫자(1만 개)라 열거하기 쉬우므로, 사용자별로 코드 입력 실패
 * (없는 코드 / 내 코드 / 차단 관계 / 비활성 사용자)를 세고 한도를 넘으면 429로 막는다.
 * 카운터는 Redis에 두고 TTL(기본 1시간)이 지나면 자동으로 초기화된다.
 *
 * Redis 장애 시에는 제한을 건너뛰고 요청을 통과시킨다. (기존 DrinkReportCacheService와 같은 폴백 방침 —
 * Redis가 죽었다고 친구 요청 기능 전체가 막히지 않게 하되, 그동안은 제한이 동작하지 않는다.)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FriendCodeAttemptLimiter {

    private static final String KEY_PREFIX = "friend:code-fail:";

    private final StringRedisTemplate redisTemplate;

    // 창(window) 안에서 허용하는 실패 횟수. 초과하면 이후 요청은 429. (명세 부록 #1: 시간당 실패 10회)
    @Value("${friend.code-attempt.max-failures:10}")
    private int maxFailures = 10;

    @Value("${friend.code-attempt.window:1h}")
    private Duration window = Duration.ofHours(1);

    /** 실패 횟수가 한도에 도달했으면 429를 던진다. 코드 조회 전에 호출한다. */
    public void assertNotLimited(Long userId) {
        try {
            String value = redisTemplate.opsForValue().get(buildKey(userId));
            if (value != null && Long.parseLong(value) >= maxFailures) {
                throw new GeneralException(FriendErrorStatus.TOO_MANY_ATTEMPTS);
            }
        } catch (GeneralException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[FRIEND CODE LIMIT] 조회 실패, 제한 없이 진행합니다: {}", e.getMessage());
        }
    }

    /** 코드 입력 실패를 1회 기록한다. */
    public void recordFailure(Long userId) {
        try {
            String key = buildKey(userId);
            redisTemplate.opsForValue().increment(key);

            // 첫 실패에서 TTL을 건다. INCR와 EXPIRE 사이에 장애가 나 TTL 없는 키가 남으면
            // 그 사용자가 영구히 막히므로, TTL이 없는 상태(-1)로 발견되면 매번 다시 건다.
            Long ttl = redisTemplate.getExpire(key);
            if (ttl != null && ttl < 0) {
                redisTemplate.expire(key, window);
            }
        } catch (Exception e) {
            log.warn("[FRIEND CODE LIMIT] 실패 기록 실패, 무시하고 진행합니다: {}", e.getMessage());
        }
    }

    private String buildKey(Long userId) {
        return KEY_PREFIX + userId;
    }
}
