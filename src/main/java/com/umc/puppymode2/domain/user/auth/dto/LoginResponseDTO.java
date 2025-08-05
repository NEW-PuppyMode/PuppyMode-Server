package com.umc.puppymode2.domain.user.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "카카오 로그인 시 JWT 응답 DTO")
public class LoginResponseDTO {

    @Schema(description = "Access Token (JWT)", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;

    @Schema(description = "Refresh Token (JWT)", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String refreshToken;

    @Schema(description = "Access Token의 만료 시간 (초 단위)", example = "3600")
    private Long expiresIn;

    @Schema(description = "로그인한 사용자 정보")
    private LoginUserInfo userInfo;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LoginUserInfo {

        @JsonIgnore
        private Long userId;

        @Schema(description = "사용자 닉네임", example = "홍길동")
        private String username;

        @Schema(description = "신규 회원 여부", example = "true")
        private Boolean isNewUser;
    }
}
