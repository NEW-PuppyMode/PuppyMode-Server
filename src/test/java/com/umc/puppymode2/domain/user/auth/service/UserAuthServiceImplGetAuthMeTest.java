package com.umc.puppymode2.domain.user.auth.service;

import com.umc.puppymode2.domain.goal.repository.UserGoalHistoryRepository;
import com.umc.puppymode2.domain.notification.repository.FcmTokenRepository;
import com.umc.puppymode2.domain.onboarding.progress.OnboardingProgressService;
import com.umc.puppymode2.domain.puppy.repository.PuppyRepository;
import com.umc.puppymode2.domain.user.auth.dto.AuthMeResponseDTO;
import com.umc.puppymode2.domain.user.entity.User;
import com.umc.puppymode2.domain.user.repository.SocialAuthRepository;
import com.umc.puppymode2.domain.user.repository.UserRepository;
import com.umc.puppymode2.global.apiPayload.code.status.ErrorStatus;
import com.umc.puppymode2.global.auth.token.JwtTokenProvider;
import com.umc.puppymode2.global.auth.token.JwtTokenService;
import com.umc.puppymode2.global.config.RedisConfig;
import com.umc.puppymode2.global.exception.GeneralException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAuthServiceImplGetAuthMeTest {

    private static final Long USER_ID = 12345L;

    @Mock private UserRepository userRepository;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private JwtTokenService jwtTokenService;
    @Mock private SocialAuthRepository socialAuthRepository;
    @Mock private RedisConfig.RedisHealthIndicator redisHealthIndicator;
    @Mock private PuppyRepository puppyRepository;
    @Mock private OnboardingProgressService onboardingProgressService;
    @Mock private UserGoalHistoryRepository userGoalHistoryRepository;
    @Mock private FcmTokenRepository fcmTokenRepository;

    @InjectMocks
    private UserAuthServiceImpl userAuthService;

    @Test
    @DisplayName("getAuthMe - userId를 문자열로 내려주고 기존 필드 값은 유지된다")
    void getAuthMe_userId를_문자열로_반환() {
        // given
        User user = mock(User.class);
        when(user.getUserId()).thenReturn(USER_ID);
        when(user.isTutorialShown()).thenReturn(true);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(puppyRepository.existsByUser_UserId(USER_ID)).thenReturn(true);
        when(onboardingProgressService.isOnboardingCompleted(user)).thenReturn(false);

        // when
        AuthMeResponseDTO response = userAuthService.getAuthMe(USER_ID);

        // then
        assertEquals("12345", response.userId());
        assertTrue(response.isOnboarded());
        assertTrue(response.isPuppyTestCompleted());
        assertFalse(response.onboardingCompleted());
        assertTrue(response.tutorialShown());
    }

    @Test
    @DisplayName("getAuthMe - 유저가 없으면 USER_NOT_FOUND")
    void getAuthMe_유저없으면_USER_NOT_FOUND() {
        // given
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        // when
        GeneralException e = assertThrows(GeneralException.class, () -> userAuthService.getAuthMe(USER_ID));

        // then
        assertEquals(ErrorStatus.USER_NOT_FOUND, e.getCode());
        verifyNoInteractions(puppyRepository, onboardingProgressService);
    }
}
