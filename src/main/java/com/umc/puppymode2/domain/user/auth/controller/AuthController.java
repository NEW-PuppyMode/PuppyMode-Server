package com.umc.puppymode2.domain.user.auth.controller;

import com.umc.puppymode2.domain.user.auth.dto.ReissueTokenRequestDTO;
import com.umc.puppymode2.domain.user.auth.dto.ReissueTokenResponseDTO;
import com.umc.puppymode2.global.apiPayload.ApiResponse;
import com.umc.puppymode2.global.apiPayload.code.status.ErrorStatus;
import com.umc.puppymode2.global.apiPayload.code.status.SuccessStatus;
import com.umc.puppymode2.global.auth.context.UserContext;
import com.umc.puppymode2.global.auth.token.JwtTokenProvider;
import com.umc.puppymode2.global.auth.token.JwtTokenService;
import com.umc.puppymode2.global.config.RequiresRedis;
import com.umc.puppymode2.global.config.swagger.ApiErrorCodeExamples;
import com.umc.puppymode2.global.config.swagger.ApiSuccessResponseExample;
import com.umc.puppymode2.global.exception.GeneralException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 인증 관련 API 컨트롤러
 *
 * <p>토큰 재발급 및 로그아웃 기능을 제공합니다.</p>
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
@Tag(name = "Auth", description = "인증 관련 API")
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtTokenService jwtTokenService;
    private final UserContext userContext;

    /**
     * Refresh Token 기반 토큰 재발급
     *
     * <p>만료된 Access Token을 갱신하기 위해 Refresh Token을 사용합니다.</p>
     *
     * <h6>처리 과정:</h6>
     * <ol>
     *   <li>요청된 Refresh Token과 Redis에 저장된 토큰 비교 (상수 시간 비교)</li>
     *   <li>일치할 경우 새로운 Access Token과 Refresh Token 발급</li>
     *   <li>기존 Refresh Token 무효화, 새 토큰으로 교체</li>
     * </ol>
     *
     * @param dto Refresh Token을 포함한 요청 DTO
     * @return 새로운 Access Token과 Refresh Token
     */
    @Operation(
            summary = "토큰 재발급",
            description = """
                    Refresh Token을 기반으로 Access Token과 Refresh Token을 재발급합니다.
                    
                    **처리 과정:**
                    1. Refresh Token 검증 (상수 시간 비교로 타이밍 공격 방지)
                    2. 새로운 Access/Refresh Token 생성
                    3. 기존 Refresh Token 무효화
                    
                    **주의:**
                    - Redis 연결 필수
                    - Refresh Token이 만료되었거나 일치하지 않으면 실패
                    - 보안을 위해 Refresh Token도 갱신됨
                    """
    )
    @PostMapping("/reissue")
    @RequiresRedis
    @ApiSuccessResponseExample(
            status = SuccessStatus.AUTH_REISSUE_SUCCESS,
            responseType = ReissueTokenResponseDTO.class
    )
    @ApiErrorCodeExamples({
            ErrorStatus.AUTH_INVALID_TOKEN,
            ErrorStatus.REDIS_CONNECTION_FAILURE,
            ErrorStatus.AUTH_REFRESH_TOKEN_INVALID
    })
    public ApiResponse<ReissueTokenResponseDTO> reissue(@RequestBody ReissueTokenRequestDTO dto) {

        String incomingRefreshToken = dto.refreshToken();

        Long userId = userContext.getCurrentUserId();
        String savedRefreshToken = jwtTokenService.getRefreshToken(userId);

        // 상수 시간 비교 (타이밍 공격 방지)
        if (savedRefreshToken == null || !MessageDigest.isEqual(
                savedRefreshToken.getBytes(StandardCharsets.UTF_8),
                incomingRefreshToken.getBytes(StandardCharsets.UTF_8)
        )) {
            log.warn("[REISSUE] 유효하지 않은 Refresh Token - userId={}", userId);
            throw new GeneralException(ErrorStatus.AUTH_REFRESH_TOKEN_INVALID);
        }

        // Refresh Token 교체
        String newAccessToken = jwtTokenProvider.generateAccessToken(userId);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(userId);
        jwtTokenService.saveRefreshToken(userId, newRefreshToken);

        Long expiresIn = jwtTokenProvider.getAccessTokenExpirySeconds();
        log.info("[REISSUE] Refresh Token 재발급 완료 - userId={}", userId);

        return ApiResponse.onSuccess(
                new ReissueTokenResponseDTO(newAccessToken, newRefreshToken, expiresIn),
                SuccessStatus.AUTH_REISSUE_SUCCESS.getCode(),
                SuccessStatus.AUTH_REISSUE_SUCCESS.getMessage()
        );
    }

    /**
     * 로그아웃
     *
     * <p>Redis에 저장된 Refresh Token을 삭제하여 로그아웃 처리합니다.</p>
     *
     * <h6>처리 과정:</h6>
     * <ol>
     *   <li>현재 사용자의 userId 추출 (Access Token에서)</li>
     *   <li>Redis에서 해당 userId의 Refresh Token 삭제</li>
     *   <li>삭제 결과 로깅</li>
     * </ol>
     *
     * <p><strong>참고:</strong> Access Token은 클라이언트에서 폐기해야 합니다.</p>
     */
    @Operation(
            summary = "로그아웃",
            description = """
                    현재 로그인된 사용자의 Refresh Token을 삭제하여 로그아웃 처리합니다.
                    
                    **처리 과정:**
                    1. Access Token에서 userId 추출
                    2. Redis에서 Refresh Token 삭제
                    3. 삭제 결과 반환
                    
                    **주의:**
                    - Redis 연결 필수
                    - Access Token은 클라이언트에서 폐기 필요
                    - 이미 로그아웃된 상태여도 성공 처리
                    """
    )
    @PostMapping("/logout")
    @RequiresRedis
    @ApiSuccessResponseExample(
            status = SuccessStatus.AUTH_LOGOUT_SUCCESS,
            responseType = Void.class
    )
    @ApiErrorCodeExamples({
            ErrorStatus.AUTH_INVALID_TOKEN,
            ErrorStatus.REDIS_CONNECTION_FAILURE
    })
    public ApiResponse<Void> logout() {

        Long userId = userContext.getCurrentUserId();
        boolean result = jwtTokenService.removeRefreshToken(userId);

        if (result) {
            log.info("[LOGOUT] RefreshToken 삭제 완료 - userId={}", userId);
        } else {
            log.info("[LOGOUT] 삭제할 RefreshToken이 없음 - userId={}", userId);
        }

        return ApiResponse.onSuccess(
                null,
                SuccessStatus.AUTH_LOGOUT_SUCCESS.getCode(),
                SuccessStatus.AUTH_LOGOUT_SUCCESS.getMessage()
        );
    }
}