package com.umc.puppymode2.domain.user.auth.service;

import com.umc.puppymode2.domain.onboarding.progress.OnboardingProgressService;
import com.umc.puppymode2.domain.puppy.repository.PuppyRepository;
import com.umc.puppymode2.domain.user.auth.dto.AuthMeResponseDTO;
import com.umc.puppymode2.domain.user.auth.dto.LoginResponseDTO;
import com.umc.puppymode2.domain.user.auth.dto.UserAuthInfoDTO;
import com.umc.puppymode2.domain.user.auth.enums.Provider;
import com.umc.puppymode2.domain.user.entity.SocialAuth;
import com.umc.puppymode2.domain.user.entity.User;
import com.umc.puppymode2.domain.user.entity.enums.UserStatus;
import com.umc.puppymode2.domain.user.repository.SocialAuthRepository;
import com.umc.puppymode2.domain.user.repository.UserRepository;
import com.umc.puppymode2.global.apiPayload.code.status.ErrorStatus;
import com.umc.puppymode2.global.auth.token.JwtTokenProvider;
import com.umc.puppymode2.global.auth.token.JwtTokenService;
import com.umc.puppymode2.global.config.RedisConfig;
import com.umc.puppymode2.global.exception.GeneralException;
import com.umc.puppymode2.global.security.UserAuthentication;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserAuthServiceImpl implements UserAuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtTokenService jwtTokenService;
    private final SocialAuthRepository socialAuthRepository;
    private final RedisConfig.RedisHealthIndicator redisHealthIndicator;
    private final PuppyRepository puppyRepository;
    private final OnboardingProgressService onboardingProgressService;

    @Transactional
    @Override
    public LoginResponseDTO createOrUpdateUser(UserAuthInfoDTO userInfo, Provider provider, String refreshToken) {
        AtomicBoolean isNewUser = new AtomicBoolean(false);

        String providerId = userInfo.getProviderId();
        String email = userInfo.getEmail();
        String newUsername = userInfo.getUsername();

        // providerId로 기존 사용자 조회 (중복 가입 방지)
        Optional<SocialAuth> optionalUserAuth = socialAuthRepository.findByProviderIdAndProvider(providerId, provider);

        SocialAuth socialAuth;
        User user;

        // 기존 회원일 경우
        if (optionalUserAuth.isPresent()) {
            socialAuth = optionalUserAuth.get();
            user = socialAuth.getUser();

            // refresh token 갱신
            if (refreshToken != null && (socialAuth.getRefreshToken() == null || !refreshToken.equals(socialAuth.getRefreshToken()))) {
                log.debug("Auth Refresh Token 갱신됨.");
                socialAuth.setRefreshToken(refreshToken);
                socialAuthRepository.save(socialAuth);
            }

            // username 변경 처리
            if (newUsername != null && !newUsername.equals(user.getUsername())) {
                log.debug("username 변경됨.");
                user.setUsername(newUsername);
                userRepository.save(user);
            }

            // 인증 객체 등록
            setAuthentication(user);

            return generateLoginResponse(user, false);
        }

        // 신규 회원일 경우
        Optional<User> optionalUser = userRepository.findByEmail(email);

        user = optionalUser.map(existingUser -> {
            // 탈퇴한 회원인 경우 -> 재활성화 처리
            if (existingUser.getStatus() == UserStatus.STOP) {

                // 상태를 NORMAL로 복구
                existingUser.setStatus(UserStatus.NORMAL);

                return userRepository.save(existingUser);
            }
            return existingUser;
        }).orElseGet(() -> {
            // 새 사용자 생성
            isNewUser.set(true);
            User newUser = User.builder()
                    .username(userInfo.getUsername() != null ? userInfo.getUsername() : "Apple 사용자")
                    .email(email)
                    .provider(provider)
                    .receiveNotifications(false)
                    .status(UserStatus.NORMAL)
                    .build();
            return userRepository.save(newUser);
        });

        LocalDateTime tokenExpiry = LocalDateTime.now().plusDays(60);

        socialAuth = SocialAuth.builder()
                .user(user)
                .provider(provider)
                .providerId(providerId)
                .refreshToken(refreshToken)
                .tokenExpiry(tokenExpiry)
                .build();
        socialAuthRepository.save(socialAuth);

        // 인증 객체 등록
        setAuthentication(user);

        return generateLoginResponse(user, isNewUser.get());
    }

    /**
     * Apple Refresh Token을 조회합니다.
     * 로그아웃 및 회원탈퇴 시 Apple에 토큰 무효화 요청을 위해 사용됩니다.
     *
     * @param userId 사용자 ID
     * @return Apple Refresh Token (없으면 null)
     */
    @Override
    public String getAppleRefreshToken(Long userId) {
        Optional<SocialAuth> socialAuth = socialAuthRepository.findByUserUserIdAndProvider(userId, Provider.APPLE);
        return socialAuth.map(SocialAuth::getRefreshToken).orElse(null);
    }

    /**
     * 인증 객체를 설정합니다.
     *
     * @param user
     */
    private void setAuthentication(User user) {
        Authentication authentication = new UserAuthentication(user.getUserId(), null, null);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.debug("[AUTH] SecurityContext에 인증 객체 설정 완료 - userId={}", user.getUserId());
    }

    /**
     * JWT 발급 및 응답 생성
     *
     * @param user
     * @param isNewUser
     * @return LoginResponseDTO
     */
    private LoginResponseDTO generateLoginResponse(User user, boolean isNewUser) {
        Long userId = user.getUserId();

        String accessToken = jwtTokenProvider.generateAccessToken(userId);
        String refreshToken = null;

        // Redis 사용 가능한 경우에만 Refresh Token 생성 및 저장
        if (redisHealthIndicator.isAvailable()) {
            refreshToken = jwtTokenProvider.generateRefreshToken(userId);
            jwtTokenService.saveRefreshToken(userId, refreshToken);
            log.info("[LOGIN] Refresh Token 발급 및 저장 완료 - userId={}", userId);
        } else {
            log.debug("[LOGIN] Redis 미사용 가능 - Refresh Token 없이 로그인 처리 - userId={}", userId);
        }

        Long expiresIn = jwtTokenProvider.getAccessTokenExpirySeconds();


        log.debug("[LOGIN] 토큰 발급 완료 - userId={}", userId);

        LoginResponseDTO.LoginUserInfo loginUserInfo = LoginResponseDTO.LoginUserInfo.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .isNewUser(isNewUser)
                .build();

        return LoginResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(expiresIn)
                .userInfo(loginUserInfo)
                .build();
    }

    /**
     * 회원탈퇴 처리
     * User 엔티티의 withdraw() 메서드를 호출하여 처리합니다.
     *
     * @param userId 사용자 ID
     */
    @Override
    @Transactional
    public void withdrawUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // User 엔티티의 withdraw() 메서드 호출
        // - 상태를 STOP으로 변경
        // - 개인정보 마스킹 (이메일, 이름)
        // - 연관 데이터 CASCADE 삭제 (socialAuths, drinkHistories, advices, puppy)
        user.withdraw();

        userRepository.save(user);
        log.info("[Withdraw] 회원탈퇴 처리 완료 - userId: {}", userId);
    }

    /**
     * 사용자의 온보딩/튜토리얼 진행 상태를 조회합니다.
     * - isOnboarded / isPuppyTestCompleted: puppy 존재 여부 (강아지 유형 검사 완료 여부)
     *   ※ isOnboarded는 구버전 클라이언트 호환을 위해 유지, 값은 isPuppyTestCompleted와 동일
     * - onboardingCompleted: 강아지이름 + 내이름 + 최초 목표설정 3단계 완료 여부
     * - tutorialShown: 튜토리얼 노출 여부
     *
     * @param userId 사용자 ID
     * @return 사용자 상태 조회 응답 DTO
     */
    @Override
    @Transactional(readOnly = true)
    public AuthMeResponseDTO getAuthMe(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        boolean isPuppyTestCompleted = puppyRepository.existsByUser_UserId(user.getUserId());
        boolean onboardingCompleted = onboardingProgressService.isOnboardingCompleted(user);

        return new AuthMeResponseDTO(
                isPuppyTestCompleted,   // isOnboarded (구버전 호환, 값 동일)
                isPuppyTestCompleted,   // isPuppyTestCompleted
                onboardingCompleted,
                user.isTutorialShown()
        );
    }
}
