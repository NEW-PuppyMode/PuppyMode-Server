package com.umc.puppymode2.domain.complaint.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 신고 접수 알림을 Slack 웹훅으로 보낸다.
 *
 * 웹훅 URL은 환경 변수(COMPLAINT_SLACK_WEBHOOK_URL)로 관리하고, 비어 있으면 보내지 않는다.
 * n8n 웹훅(Header Auth)을 거치는 경우를 위해 시크릿(COMPLAINT_SLACK_WEBHOOK_SECRET)이 있으면
 * X-Webhook-Secret 헤더로 함께 보내고, 비어 있으면 헤더를 붙이지 않는다. 시크릿은 로그에 남기지 않는다.
 * 알림은 부가 기능이라 어떤 실패도 밖으로 던지지 않는다. (신고 접수 자체는 이미 커밋된 뒤다)
 */
@Slf4j
@Component
public class ComplaintSlackSender {

    static final String SECRET_HEADER = "X-Webhook-Secret";

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final WebClient webClient;
    private final String webhookUrl;
    private final String webhookSecret;

    public ComplaintSlackSender(@Qualifier("slackWebClient") WebClient webClient,
                                @Value("${complaint.slack.webhook-url:}") String webhookUrl,
                                @Value("${complaint.slack.webhook-secret:}") String webhookSecret) {
        this.webClient = webClient;
        this.webhookUrl = webhookUrl;
        this.webhookSecret = webhookSecret;
    }

    public void send(String text) {
        send(text, Map.of());
    }

    /** text 외에 구조화된 필드(스프레드시트 열 매핑용)를 함께 보낸다. null 값은 뺀다. */
    public void send(String text, Map<String, ?> data) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.warn("[신고 Slack 알림] 웹훅 URL이 설정되지 않아 알림을 보내지 않습니다.");
            return;
        }
        try {
            webClient.post()
                    .uri(webhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> {
                        if (webhookSecret != null && !webhookSecret.isBlank()) {
                            headers.set(SECRET_HEADER, webhookSecret);
                        }
                    })
                    .bodyValue(buildBody(text, data))
                    .retrieve()
                    .toBodilessEntity()
                    .block(TIMEOUT);
        } catch (WebClientResponseException e) {
            // 예외 메시지에는 요청 URI(= 웹훅 URL)가 들어 있어서 로그에 남기지 않는다. 웹훅 URL은 그 자체로 인증 정보다.
            log.warn("[신고 Slack 알림] 전송 실패: HTTP {}", e.getStatusCode().value());
        } catch (Exception e) {
            // 마찬가지로 메시지에 URL이 들어갈 수 있어 예외 종류만 남긴다.
            log.warn("[신고 Slack 알림] 전송 실패: {}", e.getClass().getSimpleName());
        }
    }

    static Map<String, Object> buildBody(String text, Map<String, ?> data) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("text", text);
        data.forEach((key, value) -> {
            if (value != null) {
                body.put(key, value);
            }
        });
        return body;
    }
}
