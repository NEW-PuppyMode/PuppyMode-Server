package com.umc.puppymode2.domain.notification.service;

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
        return new NotificationSettingResponseDTO(user.isReceiveNotifications());
    }

    @Transactional
    public NotificationSettingResponseDTO update(Long userId, boolean receiveNotifications) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
        user.updateNotificationSetting(receiveNotifications);
        return new NotificationSettingResponseDTO(user.isReceiveNotifications());
    }
}
