package com.umc.puppymode2.domain.user.service;

import com.umc.puppymode2.domain.goal.repository.UserGoalHistoryRepository;
import com.umc.puppymode2.domain.notification.repository.FcmTokenRepository;
import com.umc.puppymode2.domain.user.entity.SocialAuth;
import com.umc.puppymode2.domain.user.entity.User;
import com.umc.puppymode2.domain.user.repository.SocialAuthRepository;
import com.umc.puppymode2.domain.user.repository.UserRepository;
import com.umc.puppymode2.domain.user.auth.enums.Provider;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserCommandServiceImpl implements UserCommandService {

    private final UserRepository userRepository;
    private final SocialAuthRepository socialAuthRepository;
    private final KakaoAuthService kakaoAuthService;
    private final UserGoalHistoryRepository userGoalHistoryRepository;
    private final FcmTokenRepository fcmTokenRepository;

    @Override
    @Transactional
    public void withdrawUser(Long userId) {
        // 1. 유저 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));

        // 2. 카카오 연동 정보 조회
        SocialAuth socialAuth = socialAuthRepository.findByUserUserIdAndProvider(userId, Provider.KAKAO)
                .orElseThrow(() -> new IllegalArgumentException("카카오 연동 정보가 없습니다."));

        // 3. 카카오 연결 끊기
        disconnectKakao(userId, socialAuth);

        // 4. UserGoalHistory 삭제
        try {
            userGoalHistoryRepository.deleteAllByUserId(userId);
        } catch (Exception e) {
            log.error("[Withdraw][KAKAO] 목표 히스토리 삭제 실패 - userId: {}", userId, e);
            throw new RuntimeException("탈퇴 처리 중 오류가 발생했습니다.", e);
        }

        // 5. FCM 토큰 삭제
        try {
            fcmTokenRepository.deleteAllByUserUserId(userId);
        } catch (Exception e) {
            log.error("[Withdraw][KAKAO] FCM 토큰 삭제 실패 - userId: {}", userId, e);
        }

        // 6. User 즉시 완전 삭제 (CASCADE로 SocialAuth/DrinkHistory/Advice/Puppy 자동 삭제)
        userRepository.delete(user);
    }

    /**
     * 카카오 연결 끊기 (Access Token 사용)
     */
    private boolean disconnectKakao(Long userId, SocialAuth socialAuth) {
        String accessToken = socialAuth.getAccessToken();

        // access token 없거나 만료 시 refresh로 재발급
        if (accessToken == null) {
            try {
                accessToken = kakaoAuthService.refreshAccessToken(userId);
                socialAuth.setAccessToken(accessToken);
                socialAuthRepository.save(socialAuth);
            } catch (Exception e) {
                log.warn("[Withdraw] Access Token 재발급 실패 - userId: {}", userId, e);
            }
        }

        // User Access Token으로 연결 해제 시도
        if (accessToken != null) {
            boolean unlinked = kakaoAuthService.disconnectKakao(accessToken);
            if (unlinked) return true;
        }

        // 카카오 연결 해제 실패 - 서비스 내 탈퇴는 진행함
        log.warn("[Withdraw] 카카오 연결 해제 실패 - userId: {} (서비스 내 탈퇴는 진행)", userId);
        return false;
    }
}
