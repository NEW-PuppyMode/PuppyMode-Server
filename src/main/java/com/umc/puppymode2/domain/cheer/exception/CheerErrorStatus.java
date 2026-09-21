package com.umc.puppymode2.domain.cheer.exception;

import com.umc.puppymode2.global.apiPayload.code.BaseErrorCode;
import com.umc.puppymode2.global.apiPayload.code.ErrorReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 응원 도메인 에러 코드. (도메인 + HTTP 상태 + 일련번호)
 * 친구 도메인의 FriendErrorStatus와 같은 방식으로, 전역 ErrorStatus를 수정하지 않고 GeneralException에 그대로 던진다.
 */
@Getter
@AllArgsConstructor
public enum CheerErrorStatus implements BaseErrorCode {

    // 응원 보내기
    INVALID_TARGET_DATE(HttpStatus.BAD_REQUEST, "CHEER4001", "지금은 응원할 수 없어요"),
    NOT_FRIENDS(HttpStatus.FORBIDDEN, "CHEER4031", "친구에게만 응원을 보낼 수 있어요"),
    TEMPLATE_NOT_FOUND(HttpStatus.NOT_FOUND, "CHEER4041", "사용할 수 없는 문구예요"),
    CHEER_ALREADY_SENT(HttpStatus.CONFLICT, "CHEER4091", "이미 응원을 보냈어요"),
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
