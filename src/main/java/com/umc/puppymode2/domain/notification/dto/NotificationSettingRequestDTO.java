package com.umc.puppymode2.domain.notification.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class NotificationSettingRequestDTO {

    private Boolean receiveNotifications;
    private Boolean receiveFriendNotifications;
}
