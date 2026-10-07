package com.umc.puppymode2.domain.block.exception;

import com.umc.puppymode2.global.apiPayload.code.BaseErrorCode;
import com.umc.puppymode2.global.apiPayload.code.ErrorReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 차단 도메인 에러 코드. (도메인 + HTTP 상태 + 일련번호)
 * 친구·응원·신고 도메인과 같은 방식으로, 전역 ErrorStatus를 수정하지 않고 GeneralException에 그대로 던진다.
 */
@Getter
@AllArgsConstructor
public enum BlockErrorStatus implements BaseErrorCode {

    // 차단하기
    CANNOT_BLOCK_SELF(HttpStatus.BAD_REQUEST, "BLOCK4001", "나는 차단할 수 없어요"),
    NOT_FRIENDS(HttpStatus.FORBIDDEN, "BLOCK4031", "친구만 차단할 수 있어요"),
    ALREADY_BLOCKED(HttpStatus.CONFLICT, "BLOCK4091", "이미 차단한 사용자예요"),

    // 차단 해제
    BLOCK_NOT_FOUND(HttpStatus.NOT_FOUND, "BLOCK4042", "차단한 사용자가 아니에요"),
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
