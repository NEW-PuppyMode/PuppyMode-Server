package com.umc.puppymode2.global.config.swagger;

import com.umc.puppymode2.global.apiPayload.code.status.ErrorStatus;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Swagger에서 API 응답 예외 코드를 문서화하기 위한 어노테이션
 *
 * 사용 예시:
 * @ApiErrorCodeExamples({
 *   ErrorStatus.USER_NOT_FOUND,
 *   ErrorStatus.REDIS_CONNECTION_FAILURE
 * })
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiErrorCodeExamples {
    ErrorStatus[] value();
}