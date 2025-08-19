package com.umc.puppymode2.domain.user.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "토큰 재발급 응답 DTO")
public record ReissueTokenResponseDTO(
        @Schema(description = "Access Token (JWT)", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        String accessToken,

        @Schema(description = "Refresh Token (JWT)", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        String refreshToken,

        @Schema(description = "Access Token의 만료 시간 (초 단위)", example = "3600")
        Long expiresIn
) {
}
