package com.umc.puppymode2.domain.user.auth.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Apple OAuth 인증을 위한 설정 클래스
 * application.yml의 auth.apple 하위 설정값을 로드합니다.
 */
@Slf4j
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "auth.apple")
public class AppleAuthConfig {

    private String teamId;
    private String clientId;
    private String audience;
    private String keyId;
    private List<String> privateKeyLines;
    private String redirectUri;

    /**
     * 애플리케이션 시작 시 필수 설정값을 검증합니다.
     */
    @PostConstruct
    public void validate() {
        log.info("[Apple Config] Apple 인증 설정 검증 시작");

        validateRequiredFields();

        log.info("[Apple Config] Apple 인증 설정 검증 완료");
        log.info("[Apple Config] - Team ID: {}", maskString(teamId));
        log.info("[Apple Config] - Client(Service) ID: {}", maskString(clientId));
        log.info("[Apple Config] - Audience(App) ID: {}", maskString(audience));
        log.info("[Apple Config] - Key ID: {}", maskString(keyId));
        log.info("[Apple Config] - Private Key Lines: {} 줄",
                privateKeyLines != null ? privateKeyLines.size() : 0);
    }

    /**
     * 필수 설정값 검증
     */
    private void validateRequiredFields() {
        if (isBlank(teamId)) {
            throw new IllegalStateException("Apple Team ID가 설정되지 않았습니다. auth.apple.team-id를 확인하세요.");
        }

        if (isBlank(clientId)) {
            throw new IllegalStateException("Apple Client ID가 설정되지 않았습니다. auth.apple.client-id를 확인하세요.");
        }

        if (isBlank(audience)) {
            throw new IllegalStateException("Apple Audience가 설정되지 않았습니다. auth.apple.audience를 확인하세요.");
        }

        if (isBlank(keyId)) {
            throw new IllegalStateException("Apple Key ID가 설정되지 않았습니다. auth.apple.key-id를 확인하세요.");
        }

        if (privateKeyLines == null || privateKeyLines.isEmpty()) {
            throw new IllegalStateException("Apple Private Key가 설정되지 않았습니다. auth.apple.private-key-lines를 확인하세요.");
        }

        if (isBlank(redirectUri)) {
            throw new IllegalStateException("Apple Redirect URI가 설정되지 않았습니다. auth.apple.redirect-uri를 확인하세요.");
        }

        log.info("[Apple Config] 필수 설정값 검증 완료");
    }

    /**
     * 문자열이 비어있는지 확인합니다.
     */
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * 민감정보 로그에 출력할 때 마스킹 처리합니다.
     */
    private String maskString(String value) {
        if (value == null || value.length() <= 4) {
            return "****";
        }
        return value.substring(0, 4) + "****";
    }
}