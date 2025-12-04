package com.umc.puppymode2.global.auth.token;

import com.umc.puppymode2.global.apiPayload.code.status.ErrorStatus;
import com.umc.puppymode2.global.config.RequiresRedis;
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
    @RequiresRedis
    public void saveRefreshToken(Long userId, String refreshToken) {

        if (userId == null) {
            throw new GeneralException(ErrorStatus.USER_ID_NULL);
        }

        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            log.warn("[REDIS] No refresh token is null - userId={}", userId);
            throw new GeneralException(ErrorStatus.AUTH_REFRESH_TOKEN_INVALID);
        }

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
    @RequiresRedis
    public String getRefreshToken(Long userId) {

        if (userId == null) {
            throw new GeneralException(ErrorStatus.USER_ID_NULL);
        }

        String key = buildKey(userId);
        String token = redisTemplate.opsForValue().get(key);

        if (token == null) {
            log.warn("[REDIS] No refresh token is null - userId={}", userId);
            throw new GeneralException(ErrorStatus.AUTH_REFRESH_TOKEN_INVALID);
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
    @RequiresRedis
    public boolean removeRefreshToken(Long userId) {

        if (userId == null) {
            throw new GeneralException(ErrorStatus.USER_ID_NULL);
        }

        String key = buildKey(userId);
        Boolean result = redisTemplate.delete(key);

        boolean isDeleted = Boolean.TRUE.equals(result);
        log.info("[REDIS] Removed refresh token - result : {} - userId={}", isDeleted, userId);

        return isDeleted;
    }

    private String buildKey(Long userId) {
        return PREFIX + userId;
    }
}
