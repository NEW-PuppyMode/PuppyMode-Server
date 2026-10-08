package com.umc.puppymode2.global.alert;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 스케줄러 실패를 n8n 웹훅으로 알린다.
 *
 * 웹훅 URL은 환경 변수(SCHEDULER_ALERT_WEBHOOK_URL)로 관리하고, 비어 있으면 보내지 않는다.
 * 시크릿(SCHEDULER_ALERT_WEBHOOK_SECRET)이 있으면 X-Webhook-Secret 헤더로 함께 보낸다.
 * 알림은 부가 기능이라 어떤 실패도 밖으로 던지지 않고, URL·시크릿·예외 메시지는 로그와 알림에 남기지 않는다.
 */
@Slf4j
@Component
public class SchedulerFailureSender {

    static final String SECRET_HEADER = "X-Webhook-Secret";

    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final String APP_PACKAGE = "com.umc.puppymode2";

    private final WebClient webClient;
    private final String webhookUrl;
    private final String webhookSecret;

    public SchedulerFailureSender(@Qualifier("slackWebClient") WebClient webClient,
                                    @Value("${scheduler.alert.webhook-url:}") String webhookUrl,
                                    @Value("${scheduler.alert.webhook-secret:}") String webhookSecret) {
        this.webClient = webClient;
        this.webhookUrl = webhookUrl;
        this.webhookSecret = webhookSecret;
    }

    /** 스케줄러 이름을 직접 아는 경우(예외를 스스로 잡는 스케줄러). */
    public void notifyFailure(String jobName, Throwable cause) {
        send(buildMessage(jobName, cause), buildData(jobName, cause));
    }

    /** 스케줄러 이름을 모르는 경우(공통 에러 핸들러). 스택에서 앱 코드 첫 프레임을 발생 위치로 쓴다. */
    public void notifyFailure(Throwable cause) {
        notifyFailure(locate(cause), cause);
    }

    static String buildMessage(String jobName, Throwable cause) {
        // 예외 메시지에는 쿼리 파라미터나 URL 등이 섞일 수 있어 종류만 남긴다.
        return "⚠️ 스케줄러 실행에 실패했어요\n"
                + "• 작업: " + jobName + "\n"
                + "• 예외: " + cause.getClass().getSimpleName() + "\n"
                + "• 시각: " + LocalDateTime.now(ZoneId.of("Asia/Seoul")).withNano(0) + " (KST)";
    }

    // n8n에서 스프레드시트 열로 매핑할 구조화 필드. 예외 메시지는 넣지 않는다.
    static Map<String, Object> buildData(String jobName, Throwable cause) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("occurredAt", OffsetDateTime.now(ZoneId.of("Asia/Seoul")).withNano(0).toString());
        data.put("job", jobName);
        data.put("exception", cause.getClass().getSimpleName());
        return data;
    }

    static String locate(Throwable cause) {
        return Arrays.stream(cause.getStackTrace())
                .filter(frame -> frame.getClassName().startsWith(APP_PACKAGE))
                .findFirst()
                .map(frame -> frame.getClassName().substring(frame.getClassName().lastIndexOf('.') + 1)
                        + "." + frame.getMethodName())
                .orElse("알 수 없음");
    }

    private void send(String text, Map<String, Object> data) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.warn("[스케줄러 알림] 웹훅 URL이 설정되지 않아 알림을 보내지 않습니다.");
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
                    .bodyValue(body(text, data))
                    .retrieve()
                    .toBodilessEntity()
                    .block(TIMEOUT);
        } catch (WebClientResponseException e) {
            // 예외 메시지에는 요청 URI(= 웹훅 URL)가 들어 있어서 로그에 남기지 않는다.
            log.warn("[스케줄러 알림] 전송 실패: HTTP {}", e.getStatusCode().value());
        } catch (Exception e) {
            log.warn("[스케줄러 알림] 전송 실패: {}", e.getClass().getSimpleName());
        }
    }

    static Map<String, Object> body(String text, Map<String, Object> data) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("text", text);
        body.putAll(data);
        return body;
    }
}
