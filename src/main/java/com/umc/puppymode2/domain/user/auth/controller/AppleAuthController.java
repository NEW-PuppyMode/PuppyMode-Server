package com.umc.puppymode2.domain.user.auth.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.puppymode2.domain.user.auth.dto.AppleLoginRequestDTO;
import com.umc.puppymode2.domain.user.auth.dto.LoginResponseDTO;
import com.umc.puppymode2.domain.user.auth.service.AppleAuthService;
import com.umc.puppymode2.global.apiPayload.code.status.ErrorStatus;
import com.umc.puppymode2.global.apiPayload.code.status.SuccessStatus;
import com.umc.puppymode2.global.auth.context.UserContext;
import com.umc.puppymode2.global.config.swagger.ApiErrorCodeExamples;
import com.umc.puppymode2.global.config.swagger.ApiSuccessResponseExample;
import com.umc.puppymode2.global.apiPayload.ApiResponse;
import com.umc.puppymode2.global.exception.GeneralException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Apple 로그인 컨트롤러
 *
 * <p>Apple Sign In을 통한 OAuth 인증을 처리합니다.</p>
 *
 * <h3>주요 기능:</h3>
 * <ul>
 *   <li>providerId(sub) 기반 사용자 식별</li>
 *   <li>이메일 제공 동의 없이도 로그인 가능</li>
 *   <li>iOS/Android 앱 및 웹 테스트 지원</li>
 * </ul>
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
     * iOS/Android 앱용 애플 로그인
     *
     * <p>네이티브 앱에서 Apple Sign In을 통해 받은 Authorization Code와 Identity Token으로 로그인합니다.</p>
     *
     * <h6>처리 과정:</h6>
     * <ol>
     *   <li>Identity Token 검증 (Apple Public Key 사용)</li>
     *   <li>Authorization Code로 Apple Refresh Token 발급</li>
     *   <li>providerId(sub)로 기존 사용자 확인</li>
     *   <li>신규 사용자면 회원가입, 기존 사용자면 로그인</li>
     *   <li>서버 JWT 발급 (Access + Refresh Token)</li>
     * </ol>
     *
     * @param request Authorization Code, Identity Token, 사용자명(선택)
     * @return JWT 토큰 및 사용자 정보
     */
    @PostMapping("/login")
    @Operation(
            summary = "애플 로그인 (앱용)",
            description = """
                    애플 서버로부터 발급받은 Authorization Code와 Identity Token을 사용하여 JWT를 발급받습니다.
                    
                    **요약:**
                    - providerId(sub) 기반 사용자 식별 (이메일 동의 선택)
                    - 자동 회원가입/로그인 처리
                    - Apple Refresh Token 관리
                    
                    **인증 플로우:**
                    1. 클라이언트: Apple Sign In으로 authorizationCode, identityToken 획득
                    2. 서버: Identity Token 검증 (Apple Public Key)
                    3. 서버: Authorization Code로 Apple Refresh Token 발급
                    4. 서버: providerId(sub) 기반 사용자 식별
                    5. 서버: 신규 사용자면 회원가입, 기존 사용자면 로그인
                    6. 클라이언트: JWT 저장 및 API 호출에 사용
                    
                    **주의:**
                    - Redis 서버 장애 시 refreshToken이 null로 반환됨
                    - username은 최초 1회 애플 로그인 시에만 Apple에서 제공
                    """
    )
    @ApiSuccessResponseExample(
            status = SuccessStatus.AUTH_APPLE_LOGIN_SUCCESS,
            responseType = LoginResponseDTO.class
    )
    @ApiErrorCodeExamples({
            ErrorStatus.APPLE_MISSING_REQUIRED_FIELD,
            ErrorStatus.APPLE_AUTH_FAILED,
            ErrorStatus._INTERNAL_SERVER_ERROR
    })
    public ResponseEntity<ApiResponse<LoginResponseDTO>> appleLogin(
            @RequestBody @Valid AppleLoginRequestDTO request) {

        LoginResponseDTO response = appleAuthService.loginWithApple(
                request.getAuthorizationCode(),
                request.getIdentityToken(),
                request.getUsername()
        );

        return ResponseEntity.ok(ApiResponse.onSuccess(
                response,
                SuccessStatus.AUTH_APPLE_LOGIN_SUCCESS.getCode(),
                SuccessStatus.AUTH_APPLE_LOGIN_SUCCESS.getMessage()
        ));
    }

    /**
     * 웹 테스트용 애플 로그인 콜백
     *
     * <p>웹 Apple Sign In 테스트용 엔드포인트입니다.</p>
     *
     * <h6>동작 방식:</h6>
     * <ol>
     *   <li>Apple은 form_post 방식으로 데이터 전송</li>
     *   <li>code(Authorization Code)와 id_token(Identity Token) 수신</li>
     *   <li>user 정보(선택, 최초 로그인 시)에서 이름 추출</li>
     *   <li>앱용 로그인과 동일한 로직 수행</li>
     * </ol>
     *
     * <p><strong>주의:</strong> 배포 환경에서는 앱용 엔드포인트(/login) 사용</p>
     *
     * @param formParams Apple이 form_post로 전송한 파라미터
     * @return JWT 토큰 및 사용자 정보
     */
    @PostMapping("/callback")
    @Operation(
            summary = "애플 로그인 콜백 (웹 테스트용)",
            description = """
                    웹 방식으로 애플 로그인을 테스트합니다.
                    Apple에서 form_post로 전달하는 데이터를 처리합니다.
                    
                    **개발/테스트 전용 엔드포인트입니다.**
                    
                    `/auth/apple/login` (앱용)을 사용하세요
                    """
    )
    @ApiSuccessResponseExample(
            status = SuccessStatus.AUTH_APPLE_LOGIN_SUCCESS,
            responseType = LoginResponseDTO.class
    )
    @ApiErrorCodeExamples({
            ErrorStatus.APPLE_MISSING_REQUIRED_FIELD,
            ErrorStatus.APPLE_AUTH_FAILED,
            ErrorStatus._INTERNAL_SERVER_ERROR
    })
    public ResponseEntity<ApiResponse<LoginResponseDTO>> appleWebCallback(
            @RequestParam Map<String, String> formParams) {

        log.debug("[Apple Callback] 웹 요청 수신 - params: {}", formParams.keySet());

        if (!formParams.containsKey("code") || !formParams.containsKey("id_token")) {
            log.warn("[Apple Callback] 필수 파라미터 누락 - 존재하는 키: {}", formParams.keySet());
            throw new GeneralException(ErrorStatus.APPLE_MISSING_REQUIRED_FIELD);
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

        return ResponseEntity.ok(ApiResponse.onSuccess(
                response,
                SuccessStatus.AUTH_APPLE_LOGIN_SUCCESS.getCode(),
                SuccessStatus.AUTH_APPLE_LOGIN_SUCCESS.getMessage()
        ));
    }

    /**
     * 애플 회원탈퇴
     *
     * <p>애플 계정 연결을 해제하고 사용자 데이터를 삭제합니다.</p>
     *
     * <h6>처리 과정:</h6>
     * <ol>
     *   <li>Apple Refresh Token 무효화 시도 (Apple API 호출)</li>
     *   <li>사용자 데이터 삭제 (항상 실행, Apple API 실패해도 진행)</li>
     *   <li>연관 데이터 자동 삭제 (CASCADE)</li>
     * </ol>
     *
     * <p><strong>참고:</strong> Apple 서버 장애 시에도 사용자 탈퇴는 정상 처리됨.</p>
     */
    @DeleteMapping("/withdraw")
    @Operation(
            summary = "애플 회원탈퇴",
            description = """
                    애플 계정 연결을 해제하고 회원 정보를 삭제합니다.
                    
                    **처리 과정:**
                    1. Apple Refresh Token 무효화 시도 (Apple API)
                    2. 사용자 데이터 삭제 (DB, 항상 실행)
                    3. 연관 데이터 자동 삭제 (CASCADE)
                    
                    **주의:**
                    - Apple 서버 장애 시에도 사용자 탈퇴는 정상 처리됩니다
                    - 탈퇴 후 복구 불가능
                    """
    )
    @ApiSuccessResponseExample(
            status = SuccessStatus.AUTH_WITHDRAW_SUCCESS,
            responseType = Void.class
    )
    @ApiErrorCodeExamples({
            ErrorStatus._UNAUTHORIZED,
            ErrorStatus._INTERNAL_SERVER_ERROR
    })
    public ResponseEntity<ApiResponse<Void>> appleWithdraw() {

        Long userId = userContext.getCurrentUserId();

        // Apple 회원탈퇴 처리 (토큰 무효화 + 사용자 삭제)
        appleAuthService.withdrawAppleUser(userId);

        log.info("[Apple Withdraw] 회원탈퇴 성공 - userId: {}", userId);
        return ResponseEntity.ok(ApiResponse.onSuccess(
                null,
                SuccessStatus.AUTH_WITHDRAW_SUCCESS.getCode(),
                SuccessStatus.AUTH_WITHDRAW_SUCCESS.getMessage()
        ));
    }

    /**
     * user JSON에서 username (firstName + lastName)을 추출합니다.
     *
     * <p>Apple은 최초 로그인 시에만 user 정보를 제공합니다.
     * 이후 로그인에서는 null이 전달되므로, 클라이언트에서 별도 저장이 필요합니다.</p>
     *
     * @param userJson Apple이 제공하는 user 정보 JSON
     * @return username (firstName + lastName, 없으면 null)
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