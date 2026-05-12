package com.umc.puppymode2.domain.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FcmTokenRequestDTO {

    @NotBlank(message = "FCM 토큰은 필수입니다.")
    @Size(max = 512, message = "FCM 토큰은 512자를 초과할 수 없습니다.")
    private String fcmToken;
}
