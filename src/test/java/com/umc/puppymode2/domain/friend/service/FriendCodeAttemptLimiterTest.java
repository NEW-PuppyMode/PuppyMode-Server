package com.umc.puppymode2.domain.friend.service;

import com.umc.puppymode2.domain.friend.exception.FriendErrorStatus;
import com.umc.puppymode2.global.exception.GeneralException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FriendCodeAttemptLimiterTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private FriendCodeAttemptLimiter limiter;

    private final Long userId = 1L;
    private final String key = "friend:code-fail:1";

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void 실패_횟수가_한도_미만이면_통과한다() {
        when(valueOperations.get(key)).thenReturn("9");

        assertDoesNotThrow(() -> limiter.assertNotLimited(userId));
    }

    @Test
    void 실패_횟수가_한도에_도달하면_429_예외() {
        when(valueOperations.get(key)).thenReturn("10");

        GeneralException e = assertThrows(GeneralException.class, () -> limiter.assertNotLimited(userId));

        assertEquals(FriendErrorStatus.TOO_MANY_ATTEMPTS, e.getCode());
    }

    @Test
    void 실패_기록이_없으면_통과한다() {
        when(valueOperations.get(key)).thenReturn(null);

        assertDoesNotThrow(() -> limiter.assertNotLimited(userId));
    }

    @Test
    void Redis_장애시_제한없이_통과한다() {
        when(valueOperations.get(key)).thenThrow(new RuntimeException("redis down"));

        assertDoesNotThrow(() -> limiter.assertNotLimited(userId));
    }

    @Test
    void 첫_실패_기록시_TTL을_건다() {
        when(redisTemplate.getExpire(key)).thenReturn(-1L);

        limiter.recordFailure(userId);

        verify(valueOperations).increment(key);
        verify(redisTemplate).expire(eq(key), eq(Duration.ofHours(1)));
    }

    @Test
    void 이미_TTL이_있으면_다시_걸지_않는다() {
        when(redisTemplate.getExpire(key)).thenReturn(1800L);

        limiter.recordFailure(userId);

        verify(valueOperations).increment(key);
        verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    void 실패_기록중_Redis_장애가_나도_예외를_던지지_않는다() {
        when(valueOperations.increment(key)).thenThrow(new RuntimeException("redis down"));

        assertDoesNotThrow(() -> limiter.recordFailure(userId));
    }
}
