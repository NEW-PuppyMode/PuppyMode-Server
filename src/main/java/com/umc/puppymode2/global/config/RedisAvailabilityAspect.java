package com.umc.puppymode2.global.config;

import com.umc.puppymode2.global.apiPayload.code.status.ErrorStatus;
import com.umc.puppymode2.global.exception.RedisUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * @RequiresRedis 어노테이션이 붙은 메서드 실행 전 Redis 연결 상태를 확인하는 Aspect
 * <p>
 * Redis가 사용 불가능한 경우 RedisUnavailableException을 발생시킵니다.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RedisAvailabilityAspect {

    private final RedisConfig.RedisHealthIndicator healthIndicator;

    @Around("@annotation(com.umc.puppymode2.global.config.RequiresRedis)")
    public Object checkRedisAvailability(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!healthIndicator.isAvailable()) {
            String methodName = joinPoint.getSignature().toShortString();
            log.warn("[REDIS ASPECT] Redis 미사용 가능으로 인해 {} 호출 차단", methodName);
            throw new RedisUnavailableException(ErrorStatus.REDIS_CONNECTION_FAILURE);
        }

        return joinPoint.proceed();
    }
}