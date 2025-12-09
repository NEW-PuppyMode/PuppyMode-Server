package com.umc.puppymode2.domain.user.auth.service;

import com.umc.puppymode2.domain.user.auth.config.AppleAuthConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/**
 * Apple 인증에 필요한 Private Key를 관리하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppleKeyService {

    private final AppleAuthConfig appleAuthConfig;
    private PrivateKey cachedPrivateKey;

    /**
     * Private Key를 로드합니다.
     * 최초 호출 시 캐싱하여 재사용합니다.
     *
     * @return PrivateKey 객체
     */
    public PrivateKey getPrivateKey() {
        if (cachedPrivateKey != null) {
            return cachedPrivateKey;
        }

        try {
            String privateKeyString = appleAuthConfig.getPrivateKey();

            // PEM 포맷 처리: 헤더/푸터 제거 및 개행 제거
            String sanitizedKey = privateKeyString
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");  // 모든 공백/개행 제거

            // Base64 디코딩
            byte[] keyBytes = Base64.getDecoder().decode(sanitizedKey);

            // EC Private Key 생성
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            cachedPrivateKey = keyFactory.generatePrivate(keySpec);

            log.info("[Apple Key Service] Private Key 로드 완료");
            return cachedPrivateKey;

        } catch (Exception e) {
            log.error("[Apple Key Service] Private Key 로드 실패", e);
            throw new RuntimeException("Failed to load Apple Private Key", e);
        }
    }
}