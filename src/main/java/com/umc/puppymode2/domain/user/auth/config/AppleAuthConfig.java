package com.umc.puppymode2.domain.user.auth.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Apple OAuth 인증을 위한 설정 클래스
 * application.yml의 auth.apple 하위 설정값을 로드합니다.
 */
@Slf4j
@Getter
@Component
public class AppleAuthConfig {

    @Value("${auth.apple.team-id}")
    private String teamId;

    @Value("${auth.apple.client-id}")
    private String clientId;

    @Value("${auth.apple.key-id}")
    private String keyId;

    @Value("${auth.apple.private-key}")
    private String privateKey;

    @Value("${auth.apple.redirect-uri:https://puppy-mode.info/auth/apple/callback}")
    private String redirectUri;
}