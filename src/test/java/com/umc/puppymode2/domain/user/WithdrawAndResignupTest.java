package com.umc.puppymode2.domain.user;

import com.umc.puppymode2.domain.goal.entity.UserGoalHistory;
import com.umc.puppymode2.domain.goal.repository.UserGoalHistoryRepository;
import com.umc.puppymode2.domain.notification.entity.FcmToken;
import com.umc.puppymode2.domain.notification.repository.FcmTokenRepository;
import com.umc.puppymode2.domain.user.auth.dto.LoginResponseDTO;
import com.umc.puppymode2.domain.user.auth.dto.UserAuthInfoDTO;
import com.umc.puppymode2.domain.user.auth.enums.Provider;
import com.umc.puppymode2.domain.user.auth.service.UserAuthService;
import com.umc.puppymode2.domain.user.entity.SocialAuth;
import com.umc.puppymode2.domain.user.entity.User;
import com.umc.puppymode2.domain.user.entity.enums.UserStatus;
import com.umc.puppymode2.domain.user.repository.SocialAuthRepository;
import com.umc.puppymode2.domain.user.repository.UserRepository;
import com.umc.puppymode2.domain.user.service.KakaoAuthService;
import com.umc.puppymode2.domain.user.service.UserCommandService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.when;

@SpringBootTest
class WithdrawAndResignupTest {

    @Autowired private UserCommandService userCommandService;   // 카카오 탈퇴
    @Autowired private UserAuthService userAuthService;         // 애플 탈퇴 + 로그인/재가입
    @Autowired private UserRepository userRepository;
    @Autowired private SocialAuthRepository socialAuthRepository;
    @Autowired private FcmTokenRepository fcmTokenRepository;
    @Autowired private UserGoalHistoryRepository userGoalHistoryRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    @MockitoBean private KakaoAuthService kakaoAuthService;

    private Long cleanupUserId;

    @AfterEach
    void cleanup() {
        if (cleanupUserId == null) {
            return;
        }
        Long targetUserId = cleanupUserId;
        cleanupUserId = null;

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.executeWithoutResult(status -> {
            userRepository.findById(targetUserId).ifPresent(u -> {
                fcmTokenRepository.deleteAllByUserUserId(u.getUserId());
                userGoalHistoryRepository.deleteAllByUserId(u.getUserId());
                userRepository.delete(u);
            });
        });
    }

    @Test
    @DisplayName("카카오 탈퇴 - FCM토큰/목표히스토리 있어도 에러 없이 전부 삭제된다")
    void 카카오_탈퇴_FK_문제없이_전체삭제() {
        // given
        User user = userRepository.save(User.builder()
                .username("테스트유저-카카오")
                .email("test-kakao-" + System.currentTimeMillis() + "@test.com")
                .provider(Provider.KAKAO)
                .status(UserStatus.NORMAL)
                .receiveNotifications(true)
                .build());
        cleanupUserId = user.getUserId();

        socialAuthRepository.save(SocialAuth.builder()
                .user(user)
                .provider(Provider.KAKAO)
                .providerId("kakao-test-id-" + user.getUserId())
                .accessToken("dummy-access-token")
                .refreshToken("dummy-refresh-token")
                .tokenExpiry(LocalDateTime.now().plusHours(1))
                .build());

        fcmTokenRepository.save(new FcmToken(user, "fcm-token-kakao-" + user.getUserId()));

        userGoalHistoryRepository.save(UserGoalHistory.builder()
                .userId(user.getUserId())
                .goalMonth(LocalDate.now().withDayOfMonth(1))
                .monthlyGoalCount(10)
                .goalSetAt(LocalDateTime.now())
                .rewarded(false)
                .build());

        when(kakaoAuthService.disconnectKakao(ArgumentMatchers.anyString())).thenReturn(true);

        // when & then: FK 위반 등으로 예외가 나면 여기서 바로 실패로 잡힘
        assertDoesNotThrow(() -> userCommandService.withdrawUser(user.getUserId()));

        // then: 관련 테이블 전부 삭제됐는지
        assertThat(userRepository.findById(user.getUserId())).isEmpty();
        assertThat(fcmTokenRepository.findAll().stream()
                .noneMatch(f -> f.getUser().getUserId().equals(user.getUserId()))).isTrue();
        assertThat(userGoalHistoryRepository.existsByUserId(user.getUserId())).isFalse();

        cleanupUserId = null; // 삭제 확인 끝났으니 cleanup 스킵
    }

    @Test
    @DisplayName("애플 탈퇴 - FCM토큰/목표히스토리 있어도 에러 없이 전부 삭제된다")
    void 애플_탈퇴_FK_문제없이_전체삭제() {
        // given
        User user = userRepository.save(User.builder()
                .username("테스트유저-애플")
                .email("test-apple-" + System.currentTimeMillis() + "@test.com")
                .provider(Provider.APPLE)
                .status(UserStatus.NORMAL)
                .receiveNotifications(true)
                .build());
        cleanupUserId = user.getUserId();

        fcmTokenRepository.save(new FcmToken(user, "fcm-token-apple-" + user.getUserId()));

        userGoalHistoryRepository.save(UserGoalHistory.builder()
                .userId(user.getUserId())
                .goalMonth(LocalDate.now().withDayOfMonth(1))
                .monthlyGoalCount(15)
                .goalSetAt(LocalDateTime.now())
                .rewarded(false)
                .build());

        // when & then
        assertDoesNotThrow(() -> userAuthService.withdrawUser(user.getUserId()));

        // then
        assertThat(userRepository.findById(user.getUserId())).isEmpty();
        assertThat(fcmTokenRepository.findAll().stream()
                .noneMatch(f -> f.getUser().getUserId().equals(user.getUserId()))).isTrue();
        assertThat(userGoalHistoryRepository.existsByUserId(user.getUserId())).isFalse();

        cleanupUserId = null;
    }

    @Test
    @DisplayName("탈퇴 후 같은 이메일/providerId로 재가입하면 새로운 userId로 row가 생성된다")
    void 탈퇴후_재가입시_새로운_row_생성() {
        String email = "test-resignup-" + System.currentTimeMillis() + "@test.com";
        String providerId = "apple-provider-" + System.currentTimeMillis();

        // 1. 최초 가입
        UserAuthInfoDTO firstInfo = UserAuthInfoDTO.builder()
                .providerId(providerId)
                .email(email)
                .username("첫가입유저")
                .tokenExpiry(LocalDateTime.now().plusDays(30))
                .build();
        LoginResponseDTO firstLogin = userAuthService.createOrUpdateUser(firstInfo, Provider.APPLE, "dummy-refresh");
        Long firstUserId = firstLogin.getUserInfo().getUserId();
        assertThat(firstLogin.getUserInfo().getIsNewUser()).isTrue();

        // 2. 탈퇴 (하드 삭제)
        userAuthService.withdrawUser(firstUserId);
        assertThat(userRepository.findById(firstUserId)).isEmpty();

        // 3. 같은 이메일/providerId로 재가입 시도
        UserAuthInfoDTO secondInfo = UserAuthInfoDTO.builder()
                .providerId(providerId)
                .email(email)
                .username("재가입유저")
                .tokenExpiry(LocalDateTime.now().plusDays(30))
                .build();
        LoginResponseDTO secondLogin = userAuthService.createOrUpdateUser(secondInfo, Provider.APPLE, "dummy-refresh-2");
        Long secondUserId = secondLogin.getUserInfo().getUserId();

        // then: 재활성화가 아니라 완전히 새로운 신규가입으로 처리됐는지
        assertThat(secondLogin.getUserInfo().getIsNewUser()).isTrue();
        assertThat(secondUserId).isNotEqualTo(firstUserId);
        assertThat(userRepository.findById(secondUserId)).isPresent();

        cleanupUserId = secondUserId; // 테스트 후 정리 대상으로 등록
    }
}