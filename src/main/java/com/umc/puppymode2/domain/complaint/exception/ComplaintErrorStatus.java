package com.umc.puppymode2.domain.complaint.exception;

import com.umc.puppymode2.global.apiPayload.code.BaseErrorCode;
import com.umc.puppymode2.global.apiPayload.code.ErrorReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 신고 도메인 에러 코드. (도메인 + HTTP 상태 + 일련번호)
 * 친구·응원 도메인과 같은 방식으로, 전역 ErrorStatus를 수정하지 않고 GeneralException에 그대로 던진다.
 */
@Getter
@AllArgsConstructor
public enum ComplaintErrorStatus implements BaseErrorCode {

    CANNOT_COMPLAIN_SELF(HttpStatus.BAD_REQUEST, "COMPLAINT4001", "나는 신고할 수 없어요"),
    DETAIL_REQUIRED(HttpStatus.BAD_REQUEST, "COMPLAINT4002", "신고 사유를 입력해주세요"),
    TARGET_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "COMPLAINT4041", "존재하지 않는 사용자예요"),
    ALREADY_COMPLAINED(HttpStatus.CONFLICT, "COMPLAINT4091", "이미 같은 사유로 신고했어요"),
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
