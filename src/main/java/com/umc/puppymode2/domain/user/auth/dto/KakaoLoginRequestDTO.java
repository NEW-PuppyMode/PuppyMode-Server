package com.umc.puppymode2.domain.user.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "카카오 로그인 요청 DTO")
public record KakaoLoginRequestDTO(

        @NotBlank(message = "accessToken은 필수입니다.")
        @Schema(description = "카카오 서버에서 발급받은 Access Token", example = "eyJhbGciOiJIUzI1NiIs")
        String accessToken,

        @NotBlank(message = "refreshToken은 필수입니다.")
        @Schema(description = "카카오 서버에서 발급받은 Refresh Token", example = "eyJhbGciOiJIUzI1NiIs")
        String refreshToken,

        @NotBlank(message = "fcmToken은 필수입니다.")
        @Schema(description = "클라이언트의 FCM 토큰 (푸시용)", example = "fcmToken123456abc")
        String fcmToken

) {
}
