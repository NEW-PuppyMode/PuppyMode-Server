package com.umc.puppymode2.global.config.swagger;

import com.umc.puppymode2.global.apiPayload.code.status.SuccessStatus;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Swagger API 성공 응답 예시를 자동으로 생성하는 어노테이션
 *
 * <p>SuccessStatus enum과 응답 타입을 지정하면, 자동으로 Swagger 문서에
 * 실제 응답 형식과 일치하는 예시를 생성합니다.</p>
 *
 * <h6>사용 예시:</h6>
 * <pre>
 * {@code
 * @PostMapping("/login")
 * @ApiSuccessResponseExample(
 *     status = SuccessStatus.AUTH_KAKAO_LOGIN_SUCCESS,
 *     responseType = LoginResponseDTO.class
 * )
 * public ResponseEntity<ApiResponse<LoginResponseDTO>> kakaoLogin(...) {
 *     // ...
 * }
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiSuccessResponseExample {

    /**
     * 성공 응답 상태 코드 및 메시지를 정의한 SuccessStatus
     *
     * @return SuccessStatus enum 값
     */
    SuccessStatus status();

    /**
     * 응답 데이터의 타입 (result 필드에 들어갈 DTO 클래스)
     *
     * <p>응답에 데이터가 없는 경우(예: 로그아웃) Void.class 사용</p>
     *
     * @return 응답 DTO 클래스
     */
    Class<?> responseType();
}