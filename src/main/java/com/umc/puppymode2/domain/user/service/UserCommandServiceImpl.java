package com.umc.puppymode2.domain.user.service;

import com.umc.puppymode2.domain.drinkhistory.repository.DrinkHistoryRepository;
import com.umc.puppymode2.domain.goal.repository.UserGoalHistoryRepository;
import com.umc.puppymode2.domain.user.entity.SocialAuth;
import com.umc.puppymode2.domain.user.entity.User;
import com.umc.puppymode2.domain.user.entity.WithdrawnUserArchive;
import com.umc.puppymode2.domain.user.entity.enums.UserStatus;
import com.umc.puppymode2.domain.user.repository.SocialAuthRepository;
import com.umc.puppymode2.domain.user.repository.UserRepository;
import com.umc.puppymode2.domain.user.auth.enums.Provider;
import com.umc.puppymode2.domain.user.repository.WithDrawnUserArchiveRepository;
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
    private final WithDrawnUserArchiveRepository withdrawnUserArchiveRepository;
    private final DrinkHistoryRepository drinkHistoryRepository;

    @Override
    @Transactional
    public void withdrawUser(Long userId) {
        // 1. 유저 조회 및 상태 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));

        if (user.getStatus() == UserStatus.STOP) {
            throw new IllegalArgumentException("이미 탈퇴한 유저입니다.");
        }

        // 2. 카카오 연동 정보 조회
        SocialAuth socialAuth = socialAuthRepository.findByUserUserIdAndProvider(userId, Provider.KAKAO)
                .orElseThrow(() -> new IllegalArgumentException("카카오 연동 정보가 없습니다."));

        // 3. 법적 보관 데이터 생성
        Integer totalDrinkDays = getTotalDrinkDays(userId);
        WithdrawnUserArchive archive = WithdrawnUserArchive.fromUser(user, totalDrinkDays);
        withdrawnUserArchiveRepository.save(archive);
        log.info("[Withdraw] 법적 보관 데이터 생성 완료 - userId: {}", userId);

        // 4. 카카오 연결 끊기
        boolean unlinked = disconnectKakao(userId, socialAuth);

        // 5. UserGoalHistory 삭제
        try {
            userGoalHistoryRepository.deleteAllByUserId(userId);
            log.info("[Withdraw] 목표 히스토리 삭제 완료 - userId: {}", userId);
        } catch (Exception e) {
            log.error("[Withdraw] 목표 히스토리 삭제 실패 - userId: {}", userId, e);
            throw new RuntimeException("탈퇴 처리 중 오류가 발생했습니다.", e);
        }

        // 6. User 탈퇴 처리 (CASCADE로 연관 데이터를 자동 삭제함)
        user.withdraw();
        userRepository.save(user);

        log.info("[Withdraw] 탈퇴 완료 - userId: {}, kakaoUnlinked: {}", userId, unlinked);
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
            if (unlinked) {
                log.info("[Withdraw] 카카오 연결 해제 성공 - userId: {}", userId);
            }
        }

        // 카카오 연결 해제 실패 - 서비스 내 탈퇴는 진행함
        log.warn("[Withdraw] 카카오 연결 해제 실패 - userId: {} (서비스 내 탈퇴는 진행)", userId);
        return false;
    }

    /**
     * 총 음주일수 계산
     */
    private Integer getTotalDrinkDays(Long userId) {
        try {
            return (int)drinkHistoryRepository.countByUserUserIdAndIsDrinkTrue(userId);
        } catch (Exception e) {
            log.warn("[Withdraw] 음주일수 계산 실패 - userId: {}", userId, e);
            return 0;
        }
    }
}
