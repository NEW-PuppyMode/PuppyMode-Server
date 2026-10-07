package com.umc.puppymode2.domain.complaint.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicInteger;

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
                webClientReturning(calls, () -> Mono.just(ClientResponse.create(HttpStatus.OK).build())), "");

        assertDoesNotThrow(() -> sender.send("신고"));

        assertEquals(0, calls.get());
    }

    @Test
    void 웹훅_URL이_있으면_전송한다() {
        AtomicInteger calls = new AtomicInteger();
        ComplaintSlackSender sender = new ComplaintSlackSender(
                webClientReturning(calls, () -> Mono.just(ClientResponse.create(HttpStatus.OK).build())),
                "https://hooks.slack.com/services/test");

        sender.send("신고");

        assertEquals(1, calls.get());
    }

    @Test
    void Slack이_에러_응답을_줘도_예외를_던지지_않는다() {
        AtomicInteger calls = new AtomicInteger();
        ComplaintSlackSender sender = new ComplaintSlackSender(
                webClientReturning(calls, () -> Mono.just(ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR).build())),
                "https://hooks.slack.com/services/test");

        assertDoesNotThrow(() -> sender.send("신고"));
        assertEquals(1, calls.get());
    }

    @Test
    void 네트워크_오류가_나도_예외를_던지지_않는다() {
        AtomicInteger calls = new AtomicInteger();
        ComplaintSlackSender sender = new ComplaintSlackSender(
                webClientReturning(calls, () -> Mono.error(new RuntimeException("connection refused"))),
                "https://hooks.slack.com/services/test");

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
                    webhookUrl).send("신고");
            // 그 밖의 예외: 메시지에 URL이 섞여 있는 경우
            new ComplaintSlackSender(
                    webClientReturning(new AtomicInteger(), () -> Mono.error(new RuntimeException("POST " + webhookUrl + " failed"))),
                    webhookUrl).send("신고");
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
}
