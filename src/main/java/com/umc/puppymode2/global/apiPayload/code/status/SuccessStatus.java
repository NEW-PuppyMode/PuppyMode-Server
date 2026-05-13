package com.umc.puppymode2.global.apiPayload.code.status;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import com.umc.puppymode2.global.apiPayload.code.BaseCode;
import com.umc.puppymode2.global.apiPayload.code.ReasonDTO;

@Getter
@AllArgsConstructor
public enum SuccessStatus implements BaseCode {

    // 일반적인 응답
    _OK(HttpStatus.OK, "COMMON200", "성공입니다."),

    // 인증 관련 응답
    AUTH_KAKAO_LOGIN_SUCCESS(HttpStatus.OK, "AUTH_KAKAO200", "카카오 로그인 성공"),
    AUTH_APPLE_LOGIN_SUCCESS(HttpStatus.OK, "AUTH_APPLE200", "애플 로그인 성공"),
    AUTH_REISSUE_SUCCESS(HttpStatus.OK, "AUTH_REISSUE200", "토큰 재발급 성공"),
    AUTH_LOGOUT_SUCCESS(HttpStatus.OK, "AUTH_LOGOUT200", "로그아웃 성공"),
    AUTH_WITHDRAW_SUCCESS(HttpStatus.OK, "AUTH_WITHDRAW200", "회원탈퇴 성공"),
    AUTH_ME_SUCCESS(HttpStatus.OK, "AUTH_ME200", "사용자 정보 조회 성공"),

    // 음주 기록
    DRINK_HISTORY_RECORD_SUCCESS(HttpStatus.OK, "DRINK_HISTORY200", "음주 기록 성공"),

    // 음주 기록 상태 조회
    DRINK_HISTORY_STATUS_SUCCESS(HttpStatus.OK, "DRINK_HISTORY201", "음주 기록 상태 조회 성공"),

    // 캘린더 조회
    CALENDAR_GET_SUCCESS(HttpStatus.OK, "CALENDAR200", "캘린더 조회 성공"),

    // 한마디 조회
    ADVICE_GET_SUCCESS(HttpStatus.OK, "ADVICE200", "한마디 조회 성공"),

    // FCM 토큰
    FCM_TOKEN_REGISTER_SUCCESS(HttpStatus.OK, "FCM200", "FCM 토큰이 등록되었습니다."),
    FCM_TOKEN_DELETE_SUCCESS(HttpStatus.OK, "FCM201", "FCM 토큰이 삭제되었습니다."),

    // 알림 수신
    NOTIFICATION_STATUS_SUCCESS(HttpStatus.OK, "NOTIFICATION200", "알림 수신 여부 조회 성공");


    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ReasonDTO getReason() {
        return ReasonDTO.builder()
                .message(message)
                .code(code)
                .isSuccess(true)
                .build();
    }

    @Override
    public ReasonDTO getReasonHttpStatus() {
        return ReasonDTO.builder()
                .message(message)
                .code(code)
                .isSuccess(true)
                .httpStatus(httpStatus)
                .build();
    }
}