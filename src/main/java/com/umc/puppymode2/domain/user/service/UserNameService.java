package com.umc.puppymode2.domain.user.service;

import com.umc.puppymode2.domain.user.dto.UserNameUpdateRequestDto;
import com.umc.puppymode2.domain.user.entity.User;
import com.umc.puppymode2.domain.user.repository.UserRepository;
import com.umc.puppymode2.global.auth.context.UserContext;
import com.umc.puppymode2.global.exception.GeneralException;
import com.umc.puppymode2.global.apiPayload.code.status.ErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserNameService {

    private final UserRepository userRepository;
    private final UserContext userContext;

    @Transactional
    public void updateUsername(UserNameUpdateRequestDto requestDto) {
        Long userId = userContext.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        user.setUsername(requestDto.getMyName());
    }
}
