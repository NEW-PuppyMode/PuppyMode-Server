package com.umc.puppymode2.domain.user.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Apple Token API 응답 DTO
 * Apple의 /auth/token 엔드포인트로부터 받는 응답
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AppleTokenResponseDTO {

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("expires_in")
    private Long expiresIn;

    @JsonProperty("refresh_token")
    private String refreshToken;

    @JsonProperty("id_token")
    private String idToken;
}
