package com.umc.puppymode2.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
@Configuration
public class FcmConfig {

    @PostConstruct
    public void initFirebase() {
        if (!FirebaseApp.getApps().isEmpty()) {
            return;
        }

        try {
            GoogleCredentials credentials;
            String credentialsJson = System.getenv("FIREBASE_CREDENTIALS_JSON");

            if (credentialsJson != null && !credentialsJson.isBlank()) {
                // 운영: 환경변수
                credentials = GoogleCredentials.fromStream(
                        new ByteArrayInputStream(credentialsJson.getBytes(StandardCharsets.UTF_8))
                );
            } else {
                // 로컬: 파일
                InputStream stream = getClass().getClassLoader().getResourceAsStream("firebase-service-account.json");

                if (stream == null) {
                    log.warn("[FCM] 서비스 계정 키 없음. 초기화 스킵");
                    return;
                }

                credentials = GoogleCredentials.fromStream(stream);
            }

            FirebaseApp.initializeApp(FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .build());

        } catch (Exception e) {
            log.error("[FCM] 초기화 실패", e);
        }
    }
}
