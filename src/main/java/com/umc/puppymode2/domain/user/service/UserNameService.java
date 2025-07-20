package com.umc.puppymode2.domain.user.service;

import com.umc.puppymode2.domain.user.dto.UserNameUpdateRequestDto;

public interface UserNameService {
    void updateUsername(UserNameUpdateRequestDto requestDto);
}
