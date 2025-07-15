package com.umc.puppymode2.global.apiPayload.code.status;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import com.umc.puppymode2.global.apiPayload.code.BaseErrorCode;
import com.umc.puppymode2.global.apiPayload.code.ErrorReasonDTO;

@Getter
@AllArgsConstructor
public enum ErrorStatus implements BaseErrorCode {

    // 일반적인 에러
    _INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON500", "서버 에러, 관리자에게 문의 바랍니다."),
    _BAD_REQUEST(HttpStatus.BAD_REQUEST, "COMMON400", "잘못된 요청입니다."),
    _UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "COMMON401", "인증이 필요합니다."),
    _FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON403", "금지된 요청입니다."),

    // Temp 관련 에러
    TEMP_NOT_FOUND(HttpStatus.NOT_FOUND, "TEMP4041", "임시 데이터가 존재하지 않습니다."),
    TEMP_EXCEPTION(HttpStatus.BAD_REQUEST, "TEMP4001", "테스트 에러입니다."),

    // OnboardingTest 에러
    INVALID_QUESTION_ID(HttpStatus.BAD_REQUEST, "ONBOARD4001", "유효하지 않은 질문 ID입니다."),
    DUPLICATE_QUESTION_ID(HttpStatus.BAD_REQUEST, "ONBOARD4002", "질문 번호에 중복이 존재합니다."),
    MISSING_QUESTION_IDS(HttpStatus.BAD_REQUEST, "ONBOARD4003", "모든 질문(1~6번)에 대한 응답이 필요합니다."),
    INVALID_TRAIT_COMBINATION(HttpStatus.INTERNAL_SERVER_ERROR, "ONBOARD5001", "유효하지 않은 성향 조합입니다."),
  
    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER4041", "해당 사용자가 존재하지 않습니다."),
    USER_ALREADY_WITHDRAWN(HttpStatus.BAD_REQUEST, "USER_4001", "이미 탈퇴한 사용자입니다."),

    // Puppy 관련 에러
    PUPPY_LEVEL_NOT_FOUND(HttpStatus.NOT_FOUND, "PUPPY4041", "해당 강아지 레벨 정보가 존재하지 않습니다."),
    PUPPY_NOT_FOUND(HttpStatus.NOT_FOUND, "PUPPY4042", "해당 유저의 강아지 정보가 존재하지 않습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ErrorReasonDTO getReason() {
        return ErrorReasonDTO.builder()
                .message(message)
                .code(code)
                .isSuccess(false)
                .build();
    }

    @Override
    public ErrorReasonDTO getReasonHttpStatus() {
        return ErrorReasonDTO.builder()
                .message(message)
                .code(code)
                .isSuccess(false)
                .httpStatus(httpStatus)
                .build();
    }
}
