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

    // Temp 관련 응답
    TEMP_OK(HttpStatus.OK, "TEMP200", "임시 데이터 조회 성공"),

    // 로그인
    KAKAO_LOGIN_SUCCESS(HttpStatus.OK, "AUTH200", "카카오 로그인 성공");

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
