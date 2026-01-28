package com.umc.puppymode2.domain.user.auth.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Apple OAuth 인증을 위한 설정 클래스
 * application.yml의 auth.apple 하위 설정값을 로드합니다.
 * <p>
 * 애플리케이션 시작 시 필수 설정값 검증
 */
@Slf4j
@Getter
@Component
public class AppleAuthConfig {

    @Value("${auth.apple.team-id}")
    private String teamId;

    @Value("${auth.apple.client-id}")
    private String clientId;

    @Value("${auth.apple.audience}")
    private String audience;

    @Value("${auth.apple.key-id}")
    private String keyId;

    @Value("${auth.apple.private-key}")
    private String privateKey;

    @Value("${auth.apple.redirect-uri}")
    private String redirectUri;

    /**
     * 애플리케이션 시작 시 필수 설정값을 검증합니다.
     * 설정 누락 시 에러 메시지와 함께 시작 실패
     */
    @PostConstruct
    public void validate() {
        log.info("[Apple Config] Apple 인증 설정 검증 시작");

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

        if (isBlank(privateKey)) {
            throw new IllegalStateException("Apple Private Key가 설정되지 않았습니다. auth.apple.private-key를 확인하세요.");
        }

        if (isBlank(redirectUri)) {
            throw new IllegalStateException("Apple Redirect URI가 설정되지 않았습니다. auth.apple.redirect-uri를 확인하세요.");
        }

        log.info("[Apple Config] Apple 인증 설정 검증 완료 - Team: {}, Client(Service): {}, Audience(App): {}, Key: {}",
                maskString(teamId),
                maskString(clientId),
                maskString(audience),
                maskString(keyId));
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