package com.umc.puppymode2.domain.complaint.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ComplaintSlackSenderTest {

    private WebClient webClientReturning(AtomicInteger calls, java.util.function.Supplier<Mono<ClientResponse>> response) {
        return WebClient.builder()
                .exchangeFunction(request -> {
                    calls.incrementAndGet();
                    return response.get();
                })
                .build();
    }

    @Test
    void 웹훅_URL이_비어_있으면_전송하지_않는다() {
        AtomicInteger calls = new AtomicInteger();
        ComplaintSlackSender sender = new ComplaintSlackSender(
                webClientReturning(calls, () -> Mono.just(ClientResponse.create(HttpStatus.OK).build())), "", "");

        assertDoesNotThrow(() -> sender.send("신고"));

        assertEquals(0, calls.get());
    }

    @Test
    void 웹훅_URL이_있으면_전송한다() {
        AtomicInteger calls = new AtomicInteger();
        ComplaintSlackSender sender = new ComplaintSlackSender(
                webClientReturning(calls, () -> Mono.just(ClientResponse.create(HttpStatus.OK).build())),
                "https://hooks.slack.com/services/test", "");

        sender.send("신고");

        assertEquals(1, calls.get());
    }

    @Test
    void Slack이_에러_응답을_줘도_예외를_던지지_않는다() {
        AtomicInteger calls = new AtomicInteger();
        ComplaintSlackSender sender = new ComplaintSlackSender(
                webClientReturning(calls, () -> Mono.just(ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR).build())),
                "https://hooks.slack.com/services/test", "");

        assertDoesNotThrow(() -> sender.send("신고"));
        assertEquals(1, calls.get());
    }

    @Test
    void 네트워크_오류가_나도_예외를_던지지_않는다() {
        AtomicInteger calls = new AtomicInteger();
        ComplaintSlackSender sender = new ComplaintSlackSender(
                webClientReturning(calls, () -> Mono.error(new RuntimeException("connection refused"))),
                "https://hooks.slack.com/services/test", "");

        assertDoesNotThrow(() -> sender.send("신고"));
        assertEquals(1, calls.get());
    }

    @Test
    void 스프링_컨텍스트에서_빈으로_생성되고_웹훅_URL_프로퍼티가_없어도_뜬다() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean("slackWebClient", WebClient.class, () -> WebClient.create());
            context.registerBean(ComplaintSlackSender.class);
            context.refresh();

            assertNotNull(context.getBean(ComplaintSlackSender.class));
        }
    }

    @Test
    void 전송이_실패해도_로그에_웹훅_URL이_남지_않는다() {
        String webhookUrl = "https://hooks.slack.com/services/T000/B000/SECRET";
        Logger logger = (Logger) LoggerFactory.getLogger(ComplaintSlackSender.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            // 4xx/5xx 응답: WebClientResponseException의 메시지에는 요청 URI가 들어간다.
            new ComplaintSlackSender(
                    webClientReturning(new AtomicInteger(),
                            () -> Mono.just(ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR).build())),
                    webhookUrl, "").send("신고");
            // 그 밖의 예외: 메시지에 URL이 섞여 있는 경우
            new ComplaintSlackSender(
                    webClientReturning(new AtomicInteger(), () -> Mono.error(new RuntimeException("POST " + webhookUrl + " failed"))),
                    webhookUrl, "").send("신고");
        } finally {
            logger.detachAppender(appender);
        }

        assertEquals(2, appender.list.size());
        for (ILoggingEvent event : appender.list) {
            assertFalse(event.getFormattedMessage().contains("hooks.slack.com"), event.getFormattedMessage());
            assertFalse(event.getFormattedMessage().contains("SECRET"), event.getFormattedMessage());
        }
        assertTrue(appender.list.get(0).getFormattedMessage().contains("500"));
    }

    private WebClient capturingWebClient(AtomicReference<ClientRequest> captured, HttpStatus status) {
        return WebClient.builder()
                .exchangeFunction(request -> {
                    captured.set(request);
                    return Mono.just(ClientResponse.create(status).build());
                })
                .build();
    }

    @Test
    void 시크릿이_있으면_X_Webhook_Secret_헤더를_보낸다() {
        AtomicReference<ClientRequest> captured = new AtomicReference<>();
        new ComplaintSlackSender(capturingWebClient(captured, HttpStatus.OK),
                "https://n8n.example.com/webhook/test", "test-secret").send("신고");

        assertNotNull(captured.get());
        assertEquals("test-secret", captured.get().headers().getFirst(ComplaintSlackSender.SECRET_HEADER));
    }

    @Test
    void 시크릿이_비어_있으면_헤더를_붙이지_않는다() {
        AtomicReference<ClientRequest> captured = new AtomicReference<>();
        new ComplaintSlackSender(capturingWebClient(captured, HttpStatus.OK),
                "https://n8n.example.com/webhook/test", "").send("신고");

        assertNotNull(captured.get());
        assertFalse(captured.get().headers().containsKey(ComplaintSlackSender.SECRET_HEADER));
    }

    @Test
    void 전송이_실패해도_로그에_시크릿이_남지_않는다() {
        String secret = "super-secret-value";
        Logger logger = (Logger) LoggerFactory.getLogger(ComplaintSlackSender.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            new ComplaintSlackSender(capturingWebClient(new AtomicReference<>(), HttpStatus.FORBIDDEN),
                    "https://n8n.example.com/webhook/test", secret).send("신고");
        } finally {
            logger.detachAppender(appender);
        }

        assertEquals(1, appender.list.size());
        assertTrue(appender.list.get(0).getFormattedMessage().contains("403"));
        for (ILoggingEvent event : appender.list) {
            assertFalse(event.getFormattedMessage().contains(secret), event.getFormattedMessage());
        }
    }

    @Test
    void 구조화_필드가_text와_함께_본문에_실리고_null_값은_뺀다() {
        java.util.Map<String, Object> data = new java.util.LinkedHashMap<>();
        data.put("complaintId", 7L);
        data.put("targetPuppyName", null);

        java.util.Map<String, Object> body = ComplaintSlackSender.buildBody("신고", data);

        assertEquals("{text=신고, complaintId=7}", body.toString());
    }
}
