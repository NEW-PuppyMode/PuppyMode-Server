package com.umc.puppymode2.domain.notification.service;

import com.umc.puppymode2.domain.notification.dto.NotificationSettingRequestDTO;
import com.umc.puppymode2.domain.notification.dto.NotificationSettingResponseDTO;
import com.umc.puppymode2.domain.user.auth.enums.Provider;
import com.umc.puppymode2.domain.user.entity.User;
import com.umc.puppymode2.domain.user.entity.enums.UserStatus;
import com.umc.puppymode2.domain.user.repository.UserRepository;
import com.umc.puppymode2.global.exception.GeneralException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationSettingServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationSettingService service;

    private final Long userId = 1L;

    private User newUser() {
        return User.builder()
                .username("hjyoon")
                .email("hjyoon@test.com")
                .provider(Provider.KAKAO)
                .receiveNotifications(true)
                .status(UserStatus.NORMAL)
                .isCustomName(true)
                .build();
    }

    @Test
    void 알림_설정_조회시_음주기록_알림과_친구_알림을_모두_반환한다() {
        User user = newUser();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        NotificationSettingResponseDTO result = service.getStatus(userId);

        assertTrue(result.getReceiveNotifications());
        assertTrue(result.getReceiveFriendNotifications());
    }

    @Test
    void 친구_알림_필드만_보내면_음주기록_알림은_그대로_유지된다() {
        User user = newUser();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        NotificationSettingRequestDTO request = new NotificationSettingRequestDTO();
        ReflectionTestUtils.setField(request, "receiveFriendNotifications", false);

        NotificationSettingResponseDTO result = service.update(userId, request);

        assertTrue(result.getReceiveNotifications());
        assertFalse(result.getReceiveFriendNotifications());
    }

    @Test
    void 음주기록_알림_필드만_보내면_친구_알림은_그대로_유지된다() {
        User user = newUser();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        NotificationSettingRequestDTO request = new NotificationSettingRequestDTO();
        ReflectionTestUtils.setField(request, "receiveNotifications", false);

        NotificationSettingResponseDTO result = service.update(userId, request);

        assertFalse(result.getReceiveNotifications());
        assertTrue(result.getReceiveFriendNotifications());
    }

    @Test
    void 두_필드_모두_보내면_둘_다_변경된다() {
        User user = newUser();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        NotificationSettingRequestDTO request = new NotificationSettingRequestDTO();
        ReflectionTestUtils.setField(request, "receiveNotifications", false);
        ReflectionTestUtils.setField(request, "receiveFriendNotifications", false);

        NotificationSettingResponseDTO result = service.update(userId, request);

        assertFalse(result.getReceiveNotifications());
        assertFalse(result.getReceiveFriendNotifications());
    }

    @Test
    void 존재하지_않는_유저면_예외가_발생한다() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(GeneralException.class, () -> service.getStatus(userId));
    }

    @Test
    void 두_필드_모두_비어있으면_예외가_발생하고_조회조차_하지_않는다() {
        NotificationSettingRequestDTO request = new NotificationSettingRequestDTO();

        assertThrows(GeneralException.class, () -> service.update(userId, request));
        verifyNoInteractions(userRepository);
    }
}
