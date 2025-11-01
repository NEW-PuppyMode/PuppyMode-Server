//package com.umc.puppymode2.domain.user.auth.controller;
//
//import com.umc.puppymode2.domain.user.auth.dto.ReissueTokenRequestDTO;
//import com.umc.puppymode2.domain.user.auth.dto.ReissueTokenResponseDTO;
//import com.umc.puppymode2.global.apiPayload.ApiResponse;
//import com.umc.puppymode2.global.apiPayload.code.status.ErrorStatus;
//import com.umc.puppymode2.global.apiPayload.code.status.SuccessStatus;
//import com.umc.puppymode2.global.auth.context.UserContext;
//import com.umc.puppymode2.global.auth.token.JwtTokenProvider;
//import com.umc.puppymode2.global.auth.token.JwtTokenService;
//import com.umc.puppymode2.global.exception.GeneralException;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.nio.charset.StandardCharsets;
//import java.security.MessageDigest;
//
//@Slf4j
//@RequiredArgsConstructor
//@RestController
//@RequestMapping("/auth")
//@Tag(name = "Auth", description = "인증 관련 API")
//public class AuthController {
//
//    private final JwtTokenProvider jwtTokenProvider;
//    private final JwtTokenService jwtTokenService;
//    private final UserContext userContext;
//
//    /**
//     * refresh token 기반 토큰 재발급
//     * <p>
//     * - refresh 토큰을 보내면 저장된 것과 일치하는지 확인합니다.
//     * - 일치할 경우 access + refresh token을 재발급합니다.
//     * - 기존 refresh token은 무효화하고, 새 refresh token으로 덮어씁니다.
//     *
//     * @param dto ReissueTokenRequestDTO
//     * @return new access + refresh token
//     */
//    @Operation(
//            summary = "토큰 재발급",
//            description = "Refresh Token을 기반으로 Access/Refresh Token을 재발급합니다."
//    )
//    @PostMapping("/reissue")
//    public ApiResponse<ReissueTokenResponseDTO> reissue(@RequestBody ReissueTokenRequestDTO dto) {
//
//        String incomingRefreshToken = dto.refreshToken();
//
//        Long userId = userContext.getCurrentUserId();
//        String savedRefreshToken = jwtTokenService.getRefreshToken(userId);
//
//        // 상수 시간 비교
//        if (savedRefreshToken == null || !MessageDigest.isEqual(
//                savedRefreshToken.getBytes(StandardCharsets.UTF_8), // 인코딩 형식 통일
//                incomingRefreshToken.getBytes(StandardCharsets.UTF_8)
//        )) {
//            log.warn("[REISSUE] 유효하지 않은 Refresh Token - userId={}", userId);
//            throw new GeneralException(ErrorStatus.AUTH_REFRESH_TOKEN_INVALID);
//        }
//
//        // refresh token 교체
//        String newAccessToken = jwtTokenProvider.generateAccessToken(userId);
//        String newRefreshToken = jwtTokenProvider.generateRefreshToken(userId);
//        jwtTokenService.saveRefreshToken(userId, newRefreshToken);
//
//        Long expiresIn = jwtTokenProvider.getAccessTokenExpirySeconds();
//        log.info("[REISSUE] Refresh Token 재발급 완료 - userId={}", userId);
//
//        return ApiResponse.onSuccess(
//                new ReissueTokenResponseDTO(newAccessToken, newRefreshToken, expiresIn),
//                SuccessStatus.AUTH_REISSUE_SUCCESS.getCode(),
//                SuccessStatus.AUTH_REISSUE_SUCCESS.getMessage()
//        );
//    }
//
//    /**
//     * 로그아웃 처리를 합니다. (저장된 refresh token 삭제)
//     */
//    @Operation(
//            summary = "로그아웃",
//            description = "현재 로그인된 사용자의 Refresh Token을 삭제하여 로그아웃 처리합니다."
//    )
//    @PostMapping("/logout")
//    public ApiResponse<Void> logout() {
//
//        Long userId = userContext.getCurrentUserId();
//        boolean result = jwtTokenService.removeRefreshToken(userId);
//
//        if (result) {
//            log.info("[LOGOUT] RefreshToken 삭제 완료 - userId={}", userId);
//        } else {
//            log.info("[LOGOUT] 삭제할 RefreshToken이 없음 - userId={}", userId);
//        }
//
//        return ApiResponse.onSuccess(
//                null,
//                SuccessStatus.AUTH_LOGOUT_SUCCESS.getCode(),
//                SuccessStatus.AUTH_LOGOUT_SUCCESS.getMessage()
//        );
//    }
//}
