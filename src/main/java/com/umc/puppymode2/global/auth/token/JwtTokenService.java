package com.umc.puppymode2.global.auth.token;

import com.umc.puppymode2.global.apiPayload.code.status.ErrorStatus;
import com.umc.puppymode2.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@RequiredArgsConstructor
@Service
public class JwtTokenService {

    private static final String PREFIX = "RT";
    private final RedisTemplate<String, String> redisTemplate;

    private final Duration REFRESH_TOKEN_EXPIRY = Duration.ofDays(14);

    /**
     * Refresh Token을 Redis에 저장합니다.
     * <p>
     * - key: "RT:{userId}"
     * - value: refreshToken
     * - TTL: 14일
     *
     * @param userId
     * @param refreshToken
     */
    public void saveRefreshToken(Long userId, String refreshToken) {
        String key = buildKey(userId);

        redisTemplate.opsForValue().set(key, refreshToken, REFRESH_TOKEN_EXPIRY);
        log.info("[REDIS] Saved refresh token - userId={}", userId);
    }

    /**
     * Redis에서 유저의 refresh token을 조회합니다.
     *
     * @param userId
     * @return
     */
    public String getRefreshToken(Long userId) {
        String key = buildKey(userId);
        String token = redisTemplate.opsForValue().get(key);

        if (token == null) {
            log.warn("[REDIS] No refresh token found - userId={}", userId);
            throw new GeneralException(ErrorStatus.INVALID_REFRESH_TOKEN); // TODO: InvalidRefreshTokenException 구현
        }

        return token;
    }

    /**
     * Redis에서 해당 유저의 refresh token을 제거합니다.
     * <p>
     * - 로그아웃 또는 강제 만료 시 호출됩니다.
     *
     * @param userId
     */
    public void removeRefreshToken(Long userId) {
        redisTemplate.delete(buildKey(userId));
        log.info("[REDIS] Removed refresh token - userId={}", userId);
    }

    private String buildKey(Long userId) {
        return PREFIX + userId;
    }
}
