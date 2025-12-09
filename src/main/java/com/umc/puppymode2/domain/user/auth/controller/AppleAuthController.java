package com.umc.puppymode2.domain.user.auth.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.puppymode2.domain.user.auth.dto.AppleLoginRequestDTO;
import com.umc.puppymode2.domain.user.auth.dto.LoginResponseDTO;
import com.umc.puppymode2.domain.user.auth.service.AppleAuthService;
import com.umc.puppymode2.global.apiPayload.code.status.ErrorStatus;
import com.umc.puppymode2.global.auth.context.UserContext;
import com.umc.puppymode2.global.config.swagger.ApiErrorCodeExamples;
import com.umc.puppymode2.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Apple 로그인 컨트롤러
 * <p>
 * 1. Apple 소셜 로그인 구현
 * 2. providerId(sub) 기반 사용자 식별
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/apple")
@Tag(name = "Apple Auth", description = "애플 로그인 API")
public class AppleAuthController {

    private final AppleAuthService appleAuthService;
    private final ObjectMapper objectMapper;
    private final UserContext userContext;

    /**
     * iOS 앱용 애플 로그인 엔드포인트
     * JSON 요청을 받아 처리합니다.
     */
    @PostMapping("/login")
    @Operation(
            summary = "애플 로그인 (앱용)",
            description = """
                    애플 서버로부터 발급받은 Authorization Code와 Identity Token을 사용하여 JWT를 발급받습니다.
                    
                    **주요 기능:**
                    - providerId(sub) 기반 사용자 식별 (이메일 미제공 시에도 로그인 가능)
                    - 로그인 및 회원가입 자동 처리
                    
                    **앱스토어 가이드라인 준수:**
                    - 이메일 제공 동의 없이도 로그인 가능
                    - Apple Refresh Token 관리
                    """
    )
    @ApiErrorCodeExamples({
            ErrorStatus.APPLE_MISSING_REQUIRED_FIELD,
            ErrorStatus.APPLE_AUTH_FAILED,
            ErrorStatus._INTERNAL_SERVER_ERROR
    })
    public ResponseEntity<ApiResponse<LoginResponseDTO>> appleLogin(
            @RequestBody @Valid AppleLoginRequestDTO request) {
        try {
            LoginResponseDTO response = appleAuthService.loginWithApple(
                    request.getAuthorizationCode(),
                    request.getIdentityToken(),
                    request.getUsername()
            );

            return ResponseEntity.ok(ApiResponse.onSuccess(response));

        } catch (IllegalArgumentException e) {
            // 로그에 자세히, 사용자 응답은 간단히
            log.error("[Apple Login] 인증 실패 - 상세: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.onFailure("APPLE4001", "애플 로그인에 실패했습니다. 다시 시도해주세요.", null));

        } catch (Exception e) {
            // 로그에 스택트레이스, 응답은 일반
            log.error("[Apple Login] 로그인 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.onFailure("COMMON500", "로그인 처리 중 오류가 발생했습니다.", null));
        }
    }

    /**
     * 웹 테스트용 애플 로그인 엔드포인트
     * form_post 방식으로 전달되는 요청을 처리합니다.
     */
    @PostMapping("/callback")
    @Operation(
            summary = "애플 로그인 콜백 (웹 테스트용)",
            description = """
                    웹 방식으로 애플 로그인을 테스트합니다.
                    Apple에서 form_post로 전달하는 데이터를 처리합니다.
                    
                    **개발/테스트 전용 엔드포인트입니다.**
                    """
    )
    @ApiErrorCodeExamples({
            ErrorStatus.APPLE_MISSING_REQUIRED_FIELD,
            ErrorStatus.APPLE_AUTH_FAILED,
            ErrorStatus._INTERNAL_SERVER_ERROR
    })
    public ResponseEntity<ApiResponse<LoginResponseDTO>> appleWebCallback(
            @RequestParam Map<String, String> formParams) {
        try {
            log.debug("[Apple Callback] 웹 요청 수신 - params: {}", formParams.keySet());

            if (!formParams.containsKey("code") || !formParams.containsKey("id_token")) {
                log.warn("[Apple Callback] 필수 파라미터 누락: {}", formParams);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.onFailure("APPLE4002",
                                "필수 입력값이 누락되었습니다.", null));
            }

            String authorizationCode = formParams.get("code");
            String identityToken = formParams.get("id_token");
            String userJson = formParams.get("user");

            // user JSON에서 이름 추출 (최초 로그인 시에만 제공됨)
            String username = extractUsername(userJson);

            LoginResponseDTO response = appleAuthService.loginWithApple(
                    authorizationCode,
                    identityToken,
                    username
            );

            return ResponseEntity.ok(ApiResponse.onSuccess(response));

        } catch (IllegalArgumentException e) {
            log.error("[Apple Callback] 인증 실패 - 상세: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.onFailure("APPLE4001", "애플 로그인에 실패했습니다. 다시 시도해주세요.", null));

        } catch (Exception e) {
            log.error("[Apple Callback] 로그인 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.onFailure("COMMON500", "로그인 처리 중 오류가 발생했습니다.", null));
        }
    }

    /**
     * 애플 회원탈퇴
     * Sign in with Apple 제공 시 계정 삭제 기능 필수
     */
    @DeleteMapping("/withdraw")
    @Operation(
            summary = "애플 회원탈퇴",
            description = """
                    애플 계정 연결을 해제하고 회원 정보를 삭제합니다.
                    
                    **앱스토어 가이드라인 필수:**
                    - Sign in with Apple 제공 시 계정 삭제 기능 필수
                    - Apple Refresh Token 무효화
                    - 사용자 데이터 완전 삭제
                    
                    **처리 내용:**
                    1. Apple Refresh Token 무효화
                    2. 개인정보 마스킹
                    3. 연관 데이터 삭제 (CASCADE)
                    """
    )
    @ApiErrorCodeExamples({
            ErrorStatus._UNAUTHORIZED,
            ErrorStatus.APPLE_WITHDRAW_FAILED,
            ErrorStatus._INTERNAL_SERVER_ERROR
    })
    public ResponseEntity<ApiResponse<String>> appleWithdraw(Authentication authentication) {
        try {
            if (authentication == null || authentication.getPrincipal() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.onFailure("COMMON401", "인증되지 않은 사용자입니다.", null));
            }

            Long userId = userContext.getCurrentUserId();

            // Apple 회원탈퇴 처리 (토큰 무효화 + 사용자 삭제)
            appleAuthService.withdrawAppleUser(userId);

            log.info("[Apple Withdraw] 회원탈퇴 성공 - userId: {}", userId);
            return ResponseEntity.ok(ApiResponse.onSuccess("회원탈퇴가 완료되었습니다."));

        } catch (Exception e) {
            // 로그에는 자세히, 사용자에게는 간단히
            log.error("[Apple Withdraw] 회원탈퇴 실패 - 상세: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.onFailure("APPLE5001", "회원탈퇴 처리 중 오류가 발생했습니다.", null));
        }
    }

    /**
     * user JSON에서 username (firstName + lastName)을 추출합니다.
     *
     * @param userJson Apple이 제공하는 user 정보 JSON
     * @return username (없으면 null)
     */
    private String extractUsername(String userJson) {
        if (userJson == null || userJson.isEmpty()) {
            return null;
        }

        try {
            JsonNode userNode = objectMapper.readTree(userJson);
            JsonNode nameNode = userNode.get("name");

            if (nameNode == null) {
                return null;
            }

            String firstName = nameNode.has("firstName") ? nameNode.get("firstName").asText() : "";
            String lastName = nameNode.has("lastName") ? nameNode.get("lastName").asText() : "";

            String username = (firstName + " " + lastName).trim();
            return username.isEmpty() ? null : username;

        } catch (Exception e) {
            log.error("[Apple Callback] 사용자 정보 파싱 실패", e);
            return null;
        }
    }
}
