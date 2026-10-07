package com.umc.puppymode2.domain.complaint.service;

import org.junit.jupiter.api.Test;
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
}
