package com.umc.puppymode2.domain.user.service;

import com.umc.puppymode2.domain.user.entity.SocialAuth;
import com.umc.puppymode2.domain.user.entity.User;
import com.umc.puppymode2.domain.user.entity.enums.UserStatus;
import com.umc.puppymode2.domain.user.repository.SocialAuthRepository;
import com.umc.puppymode2.domain.user.repository.UserRepository;
import com.umc.puppymode2.domain.user.auth.enums.Provider;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserCommandServiceImpl implements UserCommandService {

    private final UserRepository userRepository;
    private final SocialAuthRepository socialAuthRepository;
    private final KakaoAuthService kakaoAuthService;

    @Override
    @Transactional
    public void withdrawUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));
        if (user.getStatus() == UserStatus.STOP) {
            throw new IllegalArgumentException("이미 탈퇴한 유저입니다.");
        }
        SocialAuth socialAuth = socialAuthRepository.findByUserUserIdAndProvider(userId, Provider.KAKAO)
                .orElseThrow(() -> new IllegalArgumentException("카카오 연동 정보가 없습니다."));
        String accessToken = socialAuth.getAccessToken();
        // 1. access token 없거나 만료 시 refresh로 재발급
        if (accessToken == null) {
            accessToken = kakaoAuthService.refreshAccessToken(userId);
            socialAuth.setAccessToken(accessToken);
            socialAuthRepository.save(socialAuth);
        }
        boolean unlinked = kakaoAuthService.disconnectKakao(accessToken);
        if (!unlinked) {
            // 운영 로그, 인시던트, Fallback(AdminKey) 여부 정책 검토 후
            // 서비스 내 유저는 탈퇴처리(soft delete)
        }
        user.setStatus(UserStatus.STOP);
        user.getSocialAuths().clear();
        userRepository.save(user);
    }

}
