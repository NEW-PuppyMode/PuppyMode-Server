package com.umc.puppymode2.domain.onboarding.tutorial.service;

import com.umc.puppymode2.domain.user.entity.User;
import com.umc.puppymode2.domain.user.repository.UserRepository;
import com.umc.puppymode2.global.apiPayload.code.status.ErrorStatus;
import com.umc.puppymode2.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TutorialServiceImpl implements TutorialService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public void markTutorialShown(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        // 이미 true인 경우 재저장할 필요
        if (!user.isTutorialShown()) {
            user.setTutorialShown(true);
        }
    }
}
