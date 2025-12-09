package com.umc.puppymode2.domain.user.auth.service;

import com.umc.puppymode2.domain.user.auth.config.AppleAuthConfig;
import com.umc.puppymode2.domain.user.auth.dto.AppleTokenResponseDTO;
import com.umc.puppymode2.domain.user.auth.dto.LoginResponseDTO;
import com.umc.puppymode2.domain.user.auth.dto.UserAuthInfoDTO;
import com.umc.puppymode2.domain.user.auth.enums.Provider;
import com.umc.puppymode2.global.apiPayload.code.status.ErrorStatus;
import com.umc.puppymode2.global.exception.GeneralException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.security.PrivateKey;

/**
 * Apple 로그인 처리 메인 서비스
 * <p>
 * 1. providerId(sub) 기반 사용자 식별
 * 2. 이메일 제공 동의 없이도 처리 가능
 * 3. 회원탈퇴 지원
 * 4. Refresh Token 관리
 */
@Slf4j
@Service
@Transactional
public class AppleAuthService {

    private final WebClient webClient;
    private final AppleKeyService appleKeyService;
    private final AppleAuthQueryService appleAuthQueryService;
    private final UserAuthService userAuthService;
    private final AppleAuthConfig appleAuthConfig;

    public AppleAuthService(
            @Qualifier("appleWebClient") WebClient webClient,
            AppleKeyService appleKeyService,
            AppleAuthQueryService appleAuthQueryService,
            UserAuthService userAuthService,
            AppleAuthConfig appleAuthConfig
    ) {
        this.webClient = webClient;
        this.appleKeyService = appleKeyService;
        this.appleAuthQueryService = appleAuthQueryService;
        this.userAuthService = userAuthService;
        this.appleAuthConfig = appleAuthConfig;
    }

    private static final String APPLE_TOKEN_URL = "https://appleid.apple.com/auth/token";
    private static final String APPLE_REVOKE_URL = "https://appleid.apple.com/auth/revoke";

    /**
     * Apple 로그인을 처리합니다.
     * providerId(sub)로 사용자를 식별하여 이메일 없이도 로그인 가능합니다.
     *
     * @param authorizationCode Apple Authorization Code
     * @param identityToken     Apple Identity Token (JWT)
     * @param username          사용자 이름 (선택)
     * @return 로그인 응답 (JWT 토큰 포함)
     */
    public LoginResponseDTO loginWithApple(String authorizationCode, String identityToken,
                                           String username) {
        // 1. Identity Token 검증 및 사용자 정보 추출
        Claims claims = appleAuthQueryService.verifyIdentityToken(identityToken);
        if (claims == null) {
            log.error("[Apple Login] Identity Token 검증 실패");
            throw new GeneralException(ErrorStatus.APPLE_AUTH_FAILED);
        }

        // 2. providerId(sub) 추출 - Apple의 고유 사용자 식별자
        String providerId = claims.getSubject();
        String email = claims.get("email", String.class);

        // 이메일이 없는 경우 더미 이메일 생성 (providerId 기반)
        if (email == null || email.isEmpty()) {
            email = "apple_" + providerId + "@private.com";
            log.info("[Apple Login] 이메일 미제공 - 더미 이메일 생성");
        }

        // 3. Apple Token 발급 (Refresh Token 포함)
        AppleTokenResponseDTO appleTokens = getAppleTokens(authorizationCode);
        String appleRefreshToken = appleTokens.getRefreshToken();

        // 4. 사용자 정보 DTO 생성
        UserAuthInfoDTO userInfo = UserAuthInfoDTO.builder()
                .providerId(providerId)  // sub 값 저장
                .email(email)
                .username(username)
                .build();

        // 5. 사용자 생성 또는 업데이트 (providerId 기반)
        LoginResponseDTO loginResponse = userAuthService.createOrUpdateUser(
                userInfo, Provider.APPLE, appleRefreshToken);

        log.info("[Apple Login] 로그인 성공 - providerId: {}, isNewUser: {}",
                providerId, loginResponse.getUserInfo().getIsNewUser());

        return loginResponse;
    }

    /**
     * Authorization Code를 사용하여 Apple Access Token과 Refresh Token을 발급받습니다.
     *
     * @param authorizationCode Apple Authorization Code
     * @return Apple Token 응답
     */
    private AppleTokenResponseDTO getAppleTokens(String authorizationCode) {
        if (authorizationCode == null || authorizationCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Authorization Code가 비어 있습니다.");
        }

        // Client Secret 생성
        String clientSecret = generateClientSecret();
        String clientId = appleAuthConfig.getClientId();
        String redirectUri = appleAuthConfig.getRedirectUri();

        try {
            AppleTokenResponseDTO response = webClient.post()
                    .uri(APPLE_TOKEN_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData("client_id", clientId)
                            .with("client_secret", clientSecret)
                            .with("code", authorizationCode)
                            .with("grant_type", "authorization_code")
                            .with("redirect_uri", redirectUri))
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError(), clientResponse ->
                            clientResponse.bodyToMono(String.class)
                                    .doOnNext(errorBody -> log.error("[Apple Token] 4xx 에러: {}", errorBody))
                                    .flatMap(errorBody -> Mono.error(new IllegalArgumentException(
                                            "Apple Token 발급 실패 (4xx): " + errorBody)))
                    )
                    .onStatus(status -> status.is5xxServerError(), clientResponse ->
                            clientResponse.bodyToMono(String.class)
                                    .doOnNext(errorBody -> log.error("[Apple Token] 5xx 에러: {}", errorBody))
                                    .flatMap(errorBody -> Mono.error(new RuntimeException(
                                            "Apple 서버 오류 (5xx): " + errorBody)))
                    )
                    .bodyToMono(AppleTokenResponseDTO.class)
                    .block();

            if (response == null || response.getAccessToken() == null) {
                throw new RuntimeException("Apple Token 발급 실패: 응답 없음");
            }

            log.info("[Apple Token] 토큰 발급 성공");
            return response;

        } catch (Exception e) {
            log.error("[Apple Token] 토큰 발급 중 예외 발생", e);
            throw new RuntimeException("Apple Token 발급 실패: " + e.getMessage(), e);
        }
    }

    /**
     * Client Secret을 생성합니다.
     * Apple은 ES256 알고리즘으로 서명된 JWT를 Client Secret으로 요구합니다.
     *
     * @return Client Secret (JWT)
     */
    private String generateClientSecret() {
        try {
            long now = System.currentTimeMillis();
            long exp = now + (15777000 * 1000L); // 약 6개월 후 만료

            PrivateKey privateKey = appleKeyService.getPrivateKey();
            if (privateKey == null) {
                throw new IllegalStateException("Private Key가 null입니다.");
            }

            String clientId = appleAuthConfig.getClientId();
            String keyId = appleAuthConfig.getKeyId();
            String teamId = appleAuthConfig.getTeamId();

            if (clientId == null || keyId == null || teamId == null) {
                throw new IllegalStateException("Apple 설정값(clientId, keyId, teamId)이 null입니다.");
            }

            String clientSecret = Jwts.builder()
                    .setHeaderParam("alg", "ES256")
                    .setHeaderParam("kid", keyId)
                    .setIssuer(teamId)
                    .setIssuedAt(new java.util.Date(now))
                    .setExpiration(new java.util.Date(exp))
                    .setAudience("https://appleid.apple.com")
                    .setSubject(clientId)
                    .signWith(privateKey, io.jsonwebtoken.SignatureAlgorithm.ES256)
                    .compact();

            log.debug("[Apple Auth] Client Secret 생성 완료");
            return clientSecret;

        } catch (Exception e) {
            log.error("[Apple Auth] Client Secret 생성 실패", e);
            throw new RuntimeException("Apple Client Secret 생성 실패", e);
        }
    }

    /**
     * Apple 회원탈퇴 처리
     * 앱스토어 가이드라인 준수를 위해 구현
     *
     * @param userId 사용자 ID
     */
    @Transactional
    public void withdrawAppleUser(Long userId) {
        try {
            // 1. Apple Refresh Token 무효화
            String appleRefreshToken = userAuthService.getAppleRefreshToken(userId);

            if (appleRefreshToken != null) {
                revokeAppleToken(appleRefreshToken);
                log.info("[Apple Withdraw] Apple 토큰 무효화 완료 - userId: {}", userId);
            } else {
                log.warn("[Apple Withdraw] Apple Refresh Token 없음 - userId: {}", userId);
            }

            // 2. 사용자 탈퇴 처리 (UserService에서 처리)
            userAuthService.withdrawUser(userId);

            log.info("[Apple Withdraw] 회원탈퇴 완료 - userId: {}", userId);

        } catch (Exception e) {
            log.error("[Apple Withdraw] 회원탈퇴 처리 실패 - userId: " + userId, e);
            throw new RuntimeException("회원탈퇴 처리 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * Apple Refresh Token을 무효화합니다.
     *
     * @param refreshToken Apple Refresh Token
     */
    private void revokeAppleToken(String refreshToken) {
        try {
            String clientSecret = generateClientSecret();
            String clientId = appleAuthConfig.getClientId();

            webClient.post()
                    .uri(APPLE_REVOKE_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData("client_id", clientId)
                            .with("client_secret", clientSecret)
                            .with("token", refreshToken)
                            .with("token_type_hint", "refresh_token"))
                    .retrieve()
                    .onStatus(status -> status.isError(), clientResponse ->
                            clientResponse.bodyToMono(String.class)
                                    .doOnNext(errorBody -> log.error("[Apple Revoke] 에러: {}", errorBody))
                                    .flatMap(errorBody -> Mono.error(new RuntimeException(
                                            "Apple 토큰 무효화 실패: " + errorBody)))
                    )
                    .bodyToMono(Void.class)
                    .block();

            log.info("[Apple Revoke] Apple 토큰 무효화 성공");

        } catch (Exception e) {
            log.error("[Apple Revoke] Apple 토큰 무효화 실패", e);
            throw new RuntimeException("Apple 토큰 무효화 실패", e);
        }
    }
}