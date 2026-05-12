package com.umc.puppymode2.domain.notification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FcmTokenRequestDTO {

    @NotBlank(message = "FCM 토큰은 필수입니다.")
    private String fcmToken;
}
