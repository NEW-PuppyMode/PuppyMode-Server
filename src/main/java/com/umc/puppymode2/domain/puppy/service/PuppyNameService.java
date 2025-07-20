package com.umc.puppymode2.domain.puppy.service;

import com.umc.puppymode2.domain.puppy.dto.PuppyNameRequestDto;

public interface PuppyNameService {
    void updatePuppyName(PuppyNameRequestDto requestDto);
}
