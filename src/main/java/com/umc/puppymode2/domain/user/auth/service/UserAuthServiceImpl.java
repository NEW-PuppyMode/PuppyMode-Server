package com.umc.puppymode2.domain.user.auth.service;

import com.umc.puppymode2.domain.user.auth.dto.LoginResponseDTO;
import com.umc.puppymode2.domain.user.entity.SocialAuth;
import com.umc.puppymode2.domain.user.entity.User;
import com.umc.puppymode2.domain.user.entity.enums.UserStatus;
import com.umc.puppymode2.domain.user.repository.SocialAuthRepository;
import com.umc.puppymode2.domain.user.repository.UserRepository;
import com.umc.puppymode2.global.apiPayload.code.status.ErrorStatus;
import com.umc.puppymode2.domain.user.auth.dto.UserAuthInfoDTO;
import com.umc.puppymode2.domain.user.auth.enums.Provider;
import com.umc.puppymode2.global.auth.token.JwtTokenProvider;
import com.umc.puppymode2.global.auth.token.JwtTokenService;
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

    @Transactional
    @Override
    public LoginResponseDTO createOrUpdateUser(UserAuthInfoDTO userInfo, Provider provider, String refreshToken) {
        AtomicBoolean isNewUser = new AtomicBoolean(false);

        String providerId = userInfo.getProviderId();
        String email = userInfo.getEmail();
        String newUsername = userInfo.getUsername();

        // TODO: providerId로 조회하여 중복 가입 방지 로직 추가 findByProviderIdAndProvider
        Optional<SocialAuth> optionalUserAuth = socialAuthRepository.findByUser_EmailAndProvider(email, provider);

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
                    .username(userInfo.getUsername())
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
     * 인증 객체를 설정합니다.
     *
     * @param user
     */
    private void setAuthentication(User user) {
        Authentication authentication = new UserAuthentication(user.getUserId(), null, null);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.info("[AUTH] SecurityContext에 인증 객체 설정 완료 - userId={}", user.getUserId());
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
//        String refreshToken = jwtTokenProvider.generateRefreshToken(userId);
        Long expiresIn = jwtTokenProvider.getAccessTokenExpirySeconds();

        // Redis에 refresh token 저장
//        jwtTokenService.saveRefreshToken(userId, refreshToken);

        log.info("[LOGIN] 토큰 발급 완료 - userId={}", userId);

        LoginResponseDTO.LoginUserInfo loginUserInfo = LoginResponseDTO.LoginUserInfo.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .isNewUser(isNewUser)
                .build();

        return LoginResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(null) //todo: refreshtoken으로 변경
                .expiresIn(expiresIn)
                .userInfo(loginUserInfo)
                .build();
    }
}
