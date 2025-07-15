package com.umc.puppymode2.domain.puppy.service;

import com.umc.puppymode2.domain.puppy.dto.PuppyNameRequestDto;
import com.umc.puppymode2.domain.puppy.entity.Puppy;
import com.umc.puppymode2.domain.puppy.repository.PuppyRepository;
import com.umc.puppymode2.global.apiPayload.code.status.ErrorStatus;
import com.umc.puppymode2.global.auth.context.UserContext;
import com.umc.puppymode2.global.exception.GeneralException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PuppyNameService {

    private final PuppyRepository puppyRepository;
    private final UserContext userContext;

    @Transactional
    public void updatePuppyName(PuppyNameRequestDto requestDto) {
        Long userId = userContext.getCurrentUserId();

        Puppy puppy = puppyRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.PUPPY_NOT_FOUND));

        puppy.setPuppyName(requestDto.getPuppyName());
    }
}
