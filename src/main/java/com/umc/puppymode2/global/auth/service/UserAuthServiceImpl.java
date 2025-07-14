package com.umc.puppymode2.global.auth.service;

import com.umc.puppymode2.domain.user.dto.LoginResponseDTO;
import com.umc.puppymode2.domain.user.entity.SocialAuth;
import com.umc.puppymode2.domain.user.entity.User;
import com.umc.puppymode2.domain.user.entity.enums.UserStatus;
import com.umc.puppymode2.domain.user.repository.SocialAuthRepository;
import com.umc.puppymode2.domain.user.repository.UserRepository;
import com.umc.puppymode2.global.apiPayload.code.status.ErrorStatus;
import com.umc.puppymode2.global.auth.dto.UserAuthInfoDTO;
import com.umc.puppymode2.global.auth.enums.Provider;
import com.umc.puppymode2.global.auth.token.JwtTokenProvider;
import com.umc.puppymode2.global.exception.GeneralException;
import com.umc.puppymode2.global.security.UserAuthentication;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
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

            if (refreshToken != null && (socialAuth.getRefreshToken() == null || !refreshToken.equals(socialAuth.getRefreshToken()))) {
                log.debug("Auth Refresh Token 갱신됨.");
                socialAuth.setRefreshToken(refreshToken);
                socialAuthRepository.save(socialAuth);
            }

            if (newUsername != null && !newUsername.equals(user.getUsername())) {
                log.debug("username 변경됨.");
                user.setUsername(newUsername);
                userRepository.save(user);
            }

            return generateLoginResponse(user, false);
        }

        // 신규 회원일 경우
        Optional<User> optionalUser = userRepository.findByEmail(email);

        user = optionalUser.map(existingUser -> {
            // 탈퇴한 회원인 경우
            if (existingUser.getStatus() == UserStatus.STOP) {
                throw new GeneralException(ErrorStatus.USER_ALREADY_WITHDRAWN);
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

        return generateLoginResponse(user, isNewUser.get());
    }

    /**
     * JWT 발급 및 응답 생성
     */
    private LoginResponseDTO generateLoginResponse(User user, boolean isNewUser) {
        // 인증 객체 생성
        Authentication authentication = new UserAuthentication(user.getUserId(), null, null);
        String token = jwtTokenProvider.generateToken(authentication);

        LoginResponseDTO.LoginUserInfo loginUserInfo = LoginResponseDTO.LoginUserInfo.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .isNewUser(isNewUser)
                .build();

        return LoginResponseDTO.builder()
                .accessToken(token)
                .refreshToken(null) // TODO: refresh 구현
                .userInfo(loginUserInfo)
                .build();
    }
}
