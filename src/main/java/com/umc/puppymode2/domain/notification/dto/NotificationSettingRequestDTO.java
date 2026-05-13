package com.umc.puppymode2.domain.notification.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class NotificationSettingRequestDTO {

    @NotNull(message = "알림 수신 여부는 필수입니다.")
    private Boolean receiveNotifications;
}
