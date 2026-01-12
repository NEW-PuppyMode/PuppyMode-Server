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
import com.umc.puppymode2.global.config.swagger.ApiSuccessResponseExample;
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

/**
 * 카카오 로그인 컨트롤러
 *
 * <p>카카오 OAuth 2.0을 통한 소셜 로그인을 처리합니다.</p>
 *
 * <h6>인증 흐름:</h6>
 * <ol>
 *   <li>클라이언트: 카카오 로그인으로 Access Token, Refresh Token 획득</li>
 *   <li>서버: 카카오 API로 사용자 정보 조회</li>
 *   <li>서버: 신규 사용자면 회원가입, 기존 사용자면 로그인</li>
 *   <li>서버: JWT 발급 (Access + Refresh Token)</li>
 *   <li>클라이언트: JWT 저장 및 API 호출에 사용</li>
 * </ol>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/kakao")
@Tag(name = "Kakao Login", description = "카카오 로그인 API")
public class KakaoAuthController {

    private final KakaoAuthService kakaoAuthService;
    private final UserAuthService userAuthService;

    /**
     * 카카오 로그인
     *
     * <p>카카오 서버로부터 발급받은 Access Token과 Refresh Token으로 서버 JWT를 발급받습니다.</p>
     *
     * <h6>처리 과정:</h6>
     * <ol>
     *   <li>카카오 Access Token으로 사용자 정보 조회 (카카오 API)</li>
     *   <li>providerId 기반으로 기존 사용자 확인</li>
     *   <li>신규 사용자: 회원가입 후 로그인</li>
     *   <li>기존 사용자: 로그인 처리</li>
     *   <li>서버 JWT 발급 (Access Token + Refresh Token)</li>
     * </ol>
     *
     * <p><strong>주의:</strong> Redis 서버 장애 시 refreshToken이 null로 반환됩니다.
     * 이 경우에도 accessToken만으로 API 사용은 가능하나, 토큰 재발급이 불가능합니다.</p>
     *
     * @param request 카카오 Access Token과 Refresh Token
     * @return 서버 JWT 및 사용자 정보
     */
    @PostMapping("/login")
    @Operation(
            summary = "카카오 로그인",
            description = """
                    카카오 서버로부터 발급받은 `Access Token`과 `Refresh Token`을 사용하여,
                    서버에서 JWT를 발급받는 API입니다.
                    
                    **요약:**
                    - 이미 가입된 유저면 로그인 처리
                    - 신규 유저면 회원가입 후 로그인 처리
                    
                    **인증 플로우:**
                    1. 클라이언트: 카카오 로그인으로 accessToken, refreshToken 획득
                    2. 서버: 카카오 API로 사용자 정보 조회
                    3. 서버: providerId 기반 사용자 식별
                    4. 서버: 신규 사용자면 회원가입, 기존 사용자면 로그인
                    5. 클라이언트: 서버 JWT 저장 및 API 호출에 사용
                    
                    **주의:**
                    - Redis 서버 장애 시 refreshToken이 null로 반환됩니다
                    - 이 경우에도 accessToken만으로 API 사용 가능
                    - 단, 토큰 재발급은 불가능하므로 만료 전 재로그인 필요
                    """
    )
    @ApiSuccessResponseExample(
            status = SuccessStatus.AUTH_KAKAO_LOGIN_SUCCESS,
            responseType = LoginResponseDTO.class
    )
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