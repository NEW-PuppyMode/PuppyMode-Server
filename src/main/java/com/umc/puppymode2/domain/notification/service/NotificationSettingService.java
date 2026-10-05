package com.umc.puppymode2.domain.notification.service;

import com.umc.puppymode2.domain.notification.dto.NotificationSettingRequestDTO;
import com.umc.puppymode2.domain.notification.dto.NotificationSettingResponseDTO;
import com.umc.puppymode2.domain.user.entity.User;
import com.umc.puppymode2.domain.user.repository.UserRepository;
import com.umc.puppymode2.global.apiPayload.code.status.ErrorStatus;
import com.umc.puppymode2.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationSettingService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public NotificationSettingResponseDTO getStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
        return toResponse(user);
    }

    @Transactional
    public NotificationSettingResponseDTO update(Long userId, NotificationSettingRequestDTO request) {
        if (request.getReceiveNotifications() == null && request.getReceiveFriendNotifications() == null) {
            throw new GeneralException(ErrorStatus.NOTIFICATION_SETTING_EMPTY_REQUEST);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
        if (request.getReceiveNotifications() != null) {
            user.updateNotificationSetting(request.getReceiveNotifications());
        }
        if (request.getReceiveFriendNotifications() != null) {
            user.updateFriendNotificationSetting(request.getReceiveFriendNotifications());
        }
        return toResponse(user);
    }

    private NotificationSettingResponseDTO toResponse(User user) {
        return new NotificationSettingResponseDTO(
                user.isReceiveNotifications(),
                user.isReceiveFriendNotifications()
        );
    }
}
