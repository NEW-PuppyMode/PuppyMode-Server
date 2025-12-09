package com.umc.puppymode2.domain.user.auth.service;

import com.umc.puppymode2.domain.user.auth.dto.ApplePublicKeysDTO;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.security.PublicKey;
import java.util.Map;

/**
 * Apple Identity Token을 검증하는 서비스
 */
@Slf4j
@Service
public class AppleAuthQueryService {

    private final WebClient webClient;
    private final ApplePublicKeyUtil applePublicKeyUtil;

    public AppleAuthQueryService(
            @Qualifier("appleWebClient") WebClient webClient,
            ApplePublicKeyUtil applePublicKeyUtil
    ) {
        this.webClient = webClient;
        this.applePublicKeyUtil = applePublicKeyUtil;
    }

    private static final String APPLE_PUBLIC_KEYS_URL = "https://appleid.apple.com/auth/keys";

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
                throw new IllegalArgumentException("Identity Token의 헤더에 kid 또는 alg가 없습니다.");
            }

            // Apple Public Keys 가져오기
            ApplePublicKeysDTO publicKeys = getApplePublicKeys();

            // kid가 일치하는 공개키 찾기
            ApplePublicKeysDTO.Key matchedKey = publicKeys.getKeys().stream()
                    .filter(key -> key.getKid().equals(kid))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("일치하는 Apple 공개키를 찾을 수 없습니다."));

            // PublicKey 생성
            PublicKey publicKey = applePublicKeyUtil.generatePublicKey(matchedKey);

            // Identity Token 검증 및 Claims 추출
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(publicKey)
                    .build()
                    .parseClaimsJws(identityToken)
                    .getBody();

            log.info("[Apple Auth] Identity Token 검증 완료 - sub: {}", claims.getSubject());
            return claims;

        } catch (Exception e) {
            log.error("[Apple Auth] Identity Token 검증 실패", e);
            throw new IllegalArgumentException("Invalid Apple Identity Token", e);
        }
    }

    /**
     * Identity Token의 Header를 파싱합니다.
     */
    private Map<String, String> parseHeaders(String identityToken) {
        try {
            String[] chunks = identityToken.split("\\.");
            String header = new String(java.util.Base64.getUrlDecoder().decode(chunks[0]));

            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(header, Map.class);

        } catch (Exception e) {
            log.error("[Apple Auth] Identity Token 헤더 파싱 실패", e);
            throw new RuntimeException("Failed to parse Identity Token header", e);
        }
    }

    /**
     * Apple의 공개키 목록을 가져옵니다.
     */
    private ApplePublicKeysDTO getApplePublicKeys() {
        try {
            ApplePublicKeysDTO keys = webClient.get()
                    .uri(APPLE_PUBLIC_KEYS_URL)
                    .retrieve()
                    .bodyToMono(ApplePublicKeysDTO.class)
                    .block();

            if (keys == null || keys.getKeys().isEmpty()) {
                throw new RuntimeException("Apple 공개키를 가져오지 못했습니다.");
            }

            return keys;

        } catch (Exception e) {
            log.error("[Apple Auth] Apple 공개키 조회 실패", e);
            throw new RuntimeException("Failed to fetch Apple public keys", e);
        }
    }
}