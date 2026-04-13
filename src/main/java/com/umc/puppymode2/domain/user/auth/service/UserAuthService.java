package com.umc.puppymode2.domain.user.auth.service;


import com.umc.puppymode2.domain.user.auth.dto.AuthMeResponseDTO;
import com.umc.puppymode2.domain.user.auth.dto.LoginResponseDTO;
import com.umc.puppymode2.domain.user.auth.dto.UserAuthInfoDTO;
import com.umc.puppymode2.domain.user.auth.enums.Provider;


public interface UserAuthService {

    /**
     * 새로운 회원을 생성 또는 갱신합니다.
     */
    LoginResponseDTO createOrUpdateUser(UserAuthInfoDTO userInfo, Provider authProvider, String refreshToken);

    /**
     * Apple Refresh Token을 조회합니다. (회원탈퇴 시 사용)
     */
    String getAppleRefreshToken(Long userId);

    /**
     * 회원탈퇴 처리를 합니다.
     */
    void withdrawUser(Long userId);

    /**
     * 온보딩 여부를 반환합니다.
     */
    AuthMeResponseDTO getAuthMe(Long userId);
}
