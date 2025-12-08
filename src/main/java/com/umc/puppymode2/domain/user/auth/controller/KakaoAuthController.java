package com.umc.puppymode2.domain.user.auth.controller;

import com.umc.puppymode2.domain.user.auth.dto.KakaoLoginRequestDTO;
import com.umc.puppymode2.domain.user.auth.dto.LoginResponseDTO;
import com.umc.puppymode2.domain.user.service.KakaoAuthService;
import com.umc.puppymode2.global.apiPayload.ApiResponse;
import com.umc.puppymode2.global.apiPayload.code.status.ErrorStatus;
import com.umc.puppymode2.global.apiPayload.code.status.SuccessStatus;
import com.umc.puppymode2.domain.user.auth.dto.UserAuthInfoDTO;
import com.umc.puppymode2.domain.user.auth.enums.Provider;
import com.umc.puppymode2.domain.user.auth.service.UserAuthService;
import com.umc.puppymode2.global.config.swagger.ApiErrorCodeExamples;
import io.swagger.v3.oas.annotations.Operation;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/kakao")
@Tag(name = "Kakao Login", description = "카카오 로그인 API")
public class KakaoAuthController {

    private final KakaoAuthService kakaoAuthService;
    private final UserAuthService userAuthService;

    @PostMapping("/login")
    @Operation(summary = "카카오 로그인",
            description = "카카오 서버로부터 발급받은 `Access Token`과 `Refresh Token`을 사용하여,  \n" +
                    "서버에서 JWT를 발급받는 API입니다.  \n" +
                    "* 이미 가입된 유저면 로그인 처리  \n" +
                    "* 신규 유저면 회원가입 후 로그인 처리  \n\n" +
                    "**주의:** Redis 서버 장애 시 refreshToken 없이 로그인됩니다.")
    @ApiErrorCodeExamples({
            ErrorStatus._BAD_REQUEST,
            ErrorStatus.AUTH_INVALID_TOKEN
    })
    public ResponseEntity<ApiResponse<LoginResponseDTO>> kakaoLogin(
            @Valid @RequestBody KakaoLoginRequestDTO request
    ) {
        String accessToken = request.accessToken();
        String refreshToken = request.refreshToken();

        UserAuthInfoDTO userInfo = kakaoAuthService.getUserInfo(accessToken);
        LoginResponseDTO loginResponse = userAuthService.createOrUpdateUser(
                userInfo, Provider.KAKAO, refreshToken
        );

        return ResponseEntity.ok(ApiResponse.onSuccess(
                loginResponse,
                SuccessStatus.AUTH_KAKAO_LOGIN_SUCCESS.getCode(),
                SuccessStatus.AUTH_KAKAO_LOGIN_SUCCESS.getMessage()
        ));
    }
}