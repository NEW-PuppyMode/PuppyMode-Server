package com.umc.puppymode2.global.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Redis가 필수인 API 메서드에 적용하는 어노테이션
 * <p>
 * Redis 연결이 불가능한 경우 RedisUnavailableException을 발생시킵니다.
 * <p>
 * 사용 예시:
 * <pre>
 * {@code @RequiresRedis}
 * public void someRedisOperation() {
 *     // Redis 작업
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresRedis {
}