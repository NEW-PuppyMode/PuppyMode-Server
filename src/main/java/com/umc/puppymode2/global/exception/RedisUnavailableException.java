package com.umc.puppymode2.global.exception;

import com.umc.puppymode2.global.apiPayload.code.BaseErrorCode;

/**
 * Redis 연결 불가 시 발생하는 예외
 * - Redis가 필수인 API에서 사용
 * - 503 Service Unavailable 반환
 */
public class RedisUnavailableException extends GeneralException {

    public RedisUnavailableException(BaseErrorCode code) {
        super(code);
    }
}