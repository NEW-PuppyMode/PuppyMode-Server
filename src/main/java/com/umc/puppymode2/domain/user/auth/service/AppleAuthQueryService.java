package com.umc.puppymode2.domain.user.auth.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.puppymode2.domain.user.auth.config.AppleAuthConfig;
import com.umc.puppymode2.domain.user.auth.dto.ApplePublicKeysDTO;
import com.umc.puppymode2.global.apiPayload.code.status.ErrorStatus;
import com.umc.puppymode2.global.exception.GeneralException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.security.PublicKey;
import java.util.Base64;
import java.util.Map;

/**
 * Apple Identity Token을 검증하는 서비스
 */
@Slf4j
@Service
public class AppleAuthQueryService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String APPLE_PUBLIC_KEYS_URL = "https://appleid.apple.com/auth/keys";

    private final WebClient webClient;
    private final ApplePublicKeyUtil applePublicKeyUtil;
    private final AppleAuthConfig appleAuthConfig;

    public AppleAuthQueryService(
            @Qualifier("appleWebClient") WebClient webClient,
            ApplePublicKeyUtil applePublicKeyUtil,
            AppleAuthConfig appleAuthConfig
    ) {
        this.webClient = webClient;
        this.applePublicKeyUtil = applePublicKeyUtil;
        this.appleAuthConfig = appleAuthConfig;
    }

    /**
     * Apple Identity Token을 검증합니다.
     *
     * @param identityToken Apple로부터 받은 Identity Token (JWT)
     * @return JWT Claims (사용자 정보 포함)
     */
    public Claims verifyIdentityToken(String identityToken) {
        try {
            // Identity Token의 Header에서 kid 추출
            Map<String, String> headers = parseHeaders(identityToken);
            String kid = headers.get("kid");
            String alg = headers.get("alg");

            if (kid == null || alg == null) {
                log.error("[Apple Auth] Identity Token 헤더에 kid 또는 alg가 없음");
                throw new GeneralException(ErrorStatus.APPLE_AUTH_FAILED);
            }

            // Apple Public Keys 가져오기
            ApplePublicKeysDTO publicKeys = getApplePublicKeys();

            // kid가 일치하는 공개키 찾기
            ApplePublicKeysDTO.Key matchedKey = publicKeys.getKeys().stream()
                    .filter(key -> key.getKid().equals(kid))
                    .findFirst()
                    .orElseThrow(() -> {
                        log.error("[Apple Auth] 일치하는 Apple 공개키를 찾을 수 없음 - kid: {}", kid);
                        return new GeneralException(ErrorStatus.APPLE_AUTH_FAILED);
                    });

            // PublicKey 생성
            PublicKey publicKey = applePublicKeyUtil.generatePublicKey(matchedKey);

            // Identity Token 검증 및 Claims 추출
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(publicKey)
                    .requireIssuer("https://appleid.apple.com")
                    .requireAudience(appleAuthConfig.getClientId())
                    .build()
                    .parseClaimsJws(identityToken)
                    .getBody();

            log.info("[Apple Auth] Identity Token 검증 완료 - sub: {}", claims.getSubject());
            return claims;

        } catch (GeneralException e) {
            throw e;
        } catch (io.jsonwebtoken.JwtException e) {
            log.error("[Apple Auth] JWT 검증 실패", e);
            throw new GeneralException(ErrorStatus.APPLE_AUTH_FAILED);
        } catch (Exception e) {
            log.error("[Apple Auth] Identity Token 검증 중 예외 발생", e);
            throw new GeneralException(ErrorStatus.APPLE_AUTH_FAILED);
        }
    }

    /**
     * Identity Token의 Header를 파싱합니다.
     */
    private Map<String, String> parseHeaders(String identityToken) {
        try {
            String[] chunks = identityToken.split("\\.");
            if (chunks.length < 2) {
                throw new IllegalArgumentException("Invalid JWT format");
            }

            byte[] decodedBytes = Base64.getUrlDecoder().decode(chunks[0]);
            String header = new String(decodedBytes);

            return OBJECT_MAPPER.readValue(header, new TypeReference<Map<String, String>>() {
            });

        } catch (Exception e) {
            log.error("[Apple Auth] Identity Token 헤더 파싱 실패", e);
            throw new GeneralException(ErrorStatus.APPLE_AUTH_FAILED);
        }
    }

    /**
     * Apple의 공개키 목록을 가져옵니다.
     */
    //TODO: 성능 개선을 위해 캐싱 추가 - 공개키는 자주 변경되지 않으므로 TTL과 함께 캐싱
    private ApplePublicKeysDTO getApplePublicKeys() {
        try {
            ApplePublicKeysDTO keys = webClient.get()
                    .uri(APPLE_PUBLIC_KEYS_URL)
                    .retrieve()
                    .bodyToMono(ApplePublicKeysDTO.class)
                    .timeout(java.time.Duration.ofSeconds(15))
                    .block();

            if (keys == null || keys.getKeys().isEmpty()) {
                log.error("[Apple Auth] Apple 공개키 응답이 비어있음");
                throw new GeneralException(ErrorStatus.APPLE_AUTH_FAILED);
            }

            return keys;

        } catch (Exception e) {
            log.error("[Apple Auth] Apple 공개키 조회 실패", e);
            throw new GeneralException(ErrorStatus.APPLE_AUTH_FAILED);
        }
    }
}