package com.umc.puppymode2.domain.notification.service;

import com.umc.puppymode2.domain.notification.entity.FcmToken;
import com.umc.puppymode2.domain.notification.repository.FcmTokenRepository;
import com.umc.puppymode2.domain.user.entity.User;
import com.umc.puppymode2.domain.user.repository.UserRepository;
import com.umc.puppymode2.global.apiPayload.code.status.ErrorStatus;
import com.umc.puppymode2.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class FcmTokenService {

    private final FcmTokenRepository fcmTokenRepository;
    private final UserRepository userRepository;

    public void registerToken(Long userId, String token) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        Optional<FcmToken> existing = fcmTokenRepository.findByFcmToken(token);

        if (existing.isPresent()) {
            FcmToken fcmToken = existing.get();
            // 같은 토큰이 다른 유저한테 붙어있으면 유저만 바꿈
            if (!fcmToken.getUser().getUserId().equals(userId)) {
                fcmToken.changeUser(user);
            }
        } else {
            fcmTokenRepository.save(new FcmToken(user, token));
        }
    }

    public void removeToken(Long userId, String token) {
        fcmTokenRepository.deleteByFcmTokenAndUserUserId(token, userId);
    }

    public void removeAllTokens(Long userId) {
        fcmTokenRepository.deleteAllByUserUserId(userId);
    }
}