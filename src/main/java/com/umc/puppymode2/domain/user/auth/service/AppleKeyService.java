package com.umc.puppymode2.domain.user.auth.service;

import com.umc.puppymode2.domain.user.auth.config.AppleAuthConfig;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Apple 인증에 필요한 Private Key를 관리하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppleKeyService {

    private static final Pattern BASE64_PATTERN = Pattern.compile("^[A-Za-z0-9+/=]+$");

    private final AppleAuthConfig appleAuthConfig;
    private PrivateKey cachedPrivateKey;

    /**
     * 애플리케이션 시작 시 Private Key 로드 가능 여부를 검증합니다.
     */
    @PostConstruct
    public void validatePrivateKeyOnStartup() {
        try {
            log.info("[Apple Key Service] Private Key 로드 테스트 시작");

            // 키 로드 검증
            PrivateKey testKey = getPrivateKey();

            log.info("[Apple Key Service] Private Key 로드 테스트 성공");
            log.debug("[Apple Key Service] - Key Algorithm: {}", testKey.getAlgorithm());
            log.debug("[Apple Key Service] - Key Format: {}", testKey.getFormat());

        } catch (Exception e) {
            log.error("[Apple Key Service] Private Key 로드 실패 - 애플리케이션 시작 중단", e);
            throw new IllegalStateException(
                    "Apple Private Key를 로드할 수 없습니다. " +
                            "auth.apple.private-key-lines 설정을 확인하세요.", e
            );
        }
    }

    /**
     * Private Key를 로드합니다.
     * 최초 호출 시 캐싱하여 재사용합니다.
     *
     * @return PrivateKey 객체
     */
    public PrivateKey getPrivateKey() {
        if (cachedPrivateKey != null) {
            log.info("[Apple Key Service] 캐시된 Private Key 반환");
            return cachedPrivateKey;
        }

        try {
            log.info("[Apple Key Service] Private Key 로드 시작");

            List<String> keyLines = appleAuthConfig.getPrivateKeyLines();

            if (keyLines == null || keyLines.isEmpty()) {
                throw new IllegalArgumentException("Private Key Lines가 설정되지 않았습니다.");
            }

            cachedPrivateKey = parsePrivateKey(keyLines);

            log.info("[Apple Key Service] Private Key 로드 완료");
            return cachedPrivateKey;

        } catch (Exception e) {
            log.error("[Apple Key Service] Private Key 로드 실패", e);
            throw new IllegalStateException("Failed to load Apple Private Key", e);
        }
    }

    /**
     * Private Key 파싱 및 검증
     *
     * @param keyLines Private Key를 구성하는 라인들
     * @return PrivateKey 객체
     * @throws Exception 파싱 실패 시
     */
    private PrivateKey parsePrivateKey(List<String> keyLines) throws Exception {
        log.debug("[Apple Key Service] - Private Key 줄 수: {}", keyLines.size());

        // 각 라인 결합
        String combinedKey = String.join("", keyLines);
        log.debug("[Apple Key Service] - 결합 후 총 길이: {} 문자", combinedKey.length());

        // PEM 헤더/푸터 제거 및 공백 제거
        String sanitizedKey = combinedKey
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");

        log.debug("[Apple Key Service] - 정제 후 길이: {} 문자", sanitizedKey.length());
        log.debug("[Apple Key Service] - 시작 부분: {}...",
                sanitizedKey.substring(0, Math.min(20, sanitizedKey.length())));

        // Base64 형식 검증
        if (!BASE64_PATTERN.matcher(sanitizedKey).matches()) {
            log.error(
                    "[Apple Key Service] Invalid Base64 in Apple Private Key. length={}",
                    sanitizedKey.length()
            );
            throw new IllegalArgumentException("Private Key contains invalid Base64 characters");
        }

        log.debug("[Apple Key Service] Base64 형식 검증 완료");

        // Base64 디코딩
        byte[] keyBytes = Base64.getDecoder().decode(sanitizedKey);
        log.info("[Apple Key Service] - 디코딩된 바이트 크기: {} bytes", keyBytes.length);

        // EC Private Key 생성
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

        log.debug("[Apple Key Service] - Key Algorithm: {}", privateKey.getAlgorithm());
        log.debug("[Apple Key Service] - Key Format: {}", privateKey.getFormat());

        return privateKey;
    }
}