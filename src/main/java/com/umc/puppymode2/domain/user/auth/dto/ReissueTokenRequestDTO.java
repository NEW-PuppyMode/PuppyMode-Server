package com.umc.puppymode2.domain.user.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record ReissueTokenRequestDTO(

        @NotBlank(message = "Refresh token은 필수입니다.")
        String refreshToken
) {
}
