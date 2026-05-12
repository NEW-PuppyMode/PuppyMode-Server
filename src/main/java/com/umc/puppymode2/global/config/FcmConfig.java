package com.umc.puppymode2.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
@Configuration
public class FcmConfig {

    @Value("${firebase.credentials-base64:}")
    private String credentialsBase64;

    @PostConstruct
    public void initFirebase() {
        if (!FirebaseApp.getApps().isEmpty()) return;

        if (credentialsBase64 == null || credentialsBase64.isBlank()) {
            log.warn("[FCM] 서비스 계정 키 없음. 초기화 스킵");
            return;
        }

        try {
            byte[] decoded = Base64.getDecoder().decode(credentialsBase64);
            GoogleCredentials credentials = GoogleCredentials.fromStream(
                    new ByteArrayInputStream(decoded)
            );
            FirebaseApp.initializeApp(FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .build());

        } catch (Exception e) {
            log.error("[FCM] 초기화 실패", e);
        }
    }
}
