package com.umc.puppymode2.domain.friend.exception;

import com.umc.puppymode2.global.apiPayload.code.BaseErrorCode;
import com.umc.puppymode2.global.apiPayload.code.ErrorReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 친구 도메인 에러 코드.
 *
 * 코드 규칙은 명세서를 따른다: 도메인 + HTTP 상태 + 일련번호 (예: FRIEND4091).
 * GeneralException이 BaseErrorCode를 받으므로, 전역 ErrorStatus를 수정하지 않고
 * 이 enum을 그대로 던져도 ExceptionAdvice가 기존 실패 응답 형식으로 변환해 준다.
 */
@Getter
@AllArgsConstructor
public enum FriendErrorStatus implements BaseErrorCode {

    // 친구 요청 보내기
    CANNOT_ADD_SELF(HttpStatus.BAD_REQUEST, "FRIEND4001", "내 코드는 입력할 수 없어요"),
    FRIEND_CODE_NOT_FOUND(HttpStatus.NOT_FOUND, "FRIEND4041", "존재하지 않는 코드예요"),
    ALREADY_FRIENDS(HttpStatus.CONFLICT, "FRIEND4091", "이미 친구예요"),
    REQUEST_ALREADY_SENT(HttpStatus.CONFLICT, "FRIEND4092", "이미 요청을 보냈어요"),
    TOO_MANY_ATTEMPTS(HttpStatus.TOO_MANY_REQUESTS, "FRIEND4291", "잠시 후 다시 시도해주세요"),

    // 친구 요청 수락/거절
    NOT_REQUEST_RECEIVER(HttpStatus.FORBIDDEN, "FRIEND4031", "내가 받은 요청이 아니에요"),
    REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "FRIEND4042", "존재하지 않는 요청이에요"),
    REQUEST_ALREADY_HANDLED(HttpStatus.CONFLICT, "FRIEND4093", "이미 처리된 요청이에요"),

    // 친구 프로필 / 삭제
    NOT_FRIENDS(HttpStatus.FORBIDDEN, "FRIEND4032", "친구가 아니에요"),
    FRIEND_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "FRIEND4043", "존재하지 않는 사용자예요"),
    FRIENDSHIP_NOT_FOUND(HttpStatus.NOT_FOUND, "FRIEND4044", "친구 관계가 없어요"),
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
