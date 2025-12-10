package com.umc.puppymode2.domain.user.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

/**
 * Apple 로그인 요청 DTO
 * 클라이언트에서 전달받는 Apple 인증 정보
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "애플 로그인 요청")
public class AppleLoginRequestDTO {

    @NotBlank(message = "Authorization Code는 필수입니다.")
    @Schema(description = "애플 Authorization Code", example = "c1234567890abcdef", required = true)
    private String authorizationCode;

    @NotBlank(message = "Identity Token은 필수입니다.")
    @Schema(description = "애플 Identity Token (JWT)", example = "eyJhbGciOi", required = true)
    private String identityToken;

    @Schema(description = "사용자 이름 (선택 사항, 최초 로그인 시에만 제공됨)", example = "홍길동")
    private String username;
}
