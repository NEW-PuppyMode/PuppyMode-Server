package com.umc.puppymode2.global.auth.service;


import com.umc.puppymode2.domain.user.dto.LoginResponseDTO;
import com.umc.puppymode2.global.auth.dto.UserAuthInfoDTO;
import com.umc.puppymode2.global.auth.enums.Provider;


public interface UserAuthService {

    // 새로운 회윈 생성 또는 갱신
    LoginResponseDTO createOrUpdateUser(UserAuthInfoDTO userInfo, Provider authProvider, String refreshToken);
}
