package com.umc.puppymode2.domain.user.auth.service;


import com.umc.puppymode2.domain.user.auth.dto.LoginResponseDTO;
import com.umc.puppymode2.domain.user.auth.dto.UserAuthInfoDTO;
import com.umc.puppymode2.domain.user.auth.enums.Provider;


public interface UserAuthService {

    // 새로운 회윈 생성 또는 갱신
    LoginResponseDTO createOrUpdateUser(UserAuthInfoDTO userInfo, Provider authProvider, String refreshToken);

    /**
     * Apple Refresh Token을 조회합니다. (회원탈퇴 시 사용)
     */
    String getAppleRefreshToken(Long userId);

    /**
     * 회원탈퇴 처리를 합니다.
     */
    void withdrawUser(Long userId);
}
