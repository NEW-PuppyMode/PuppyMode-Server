package com.umc.puppymode2.domain.complaint.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

/**
 * 신고 접수 알림을 Slack 웹훅으로 보낸다.
 *
 * 웹훅 URL은 환경 변수(COMPLAINT_SLACK_WEBHOOK_URL)로 관리하고, 비어 있으면 보내지 않는다.
 * 알림은 부가 기능이라 어떤 실패도 밖으로 던지지 않는다. (신고 접수 자체는 이미 커밋된 뒤다)
 */
@Slf4j
@Component
public class ComplaintSlackSender {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final WebClient webClient;
    private final String webhookUrl;

    public ComplaintSlackSender(@Qualifier("slackWebClient") WebClient webClient,
                                @Value("${complaint.slack.webhook-url:}") String webhookUrl) {
        this.webClient = webClient;
        this.webhookUrl = webhookUrl;
    }

    public void send(String text) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.warn("[신고 Slack 알림] 웹훅 URL이 설정되지 않아 알림을 보내지 않습니다.");
            return;
        }
        try {
            webClient.post()
                    .uri(webhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("text", text))
                    .retrieve()
                    .toBodilessEntity()
                    .block(TIMEOUT);
        } catch (Exception e) {
            log.warn("[신고 Slack 알림] 전송 실패: {}", e.getMessage());
        }
    }
}
