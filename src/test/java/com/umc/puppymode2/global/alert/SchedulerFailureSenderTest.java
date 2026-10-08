package com.umc.puppymode2.global.alert;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.umc.puppymode2.global.config.SchedulerConfig;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.task.ThreadPoolTaskSchedulerCustomizer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class SchedulerFailureSenderTest {

    private static final String URL = "https://n8n.example.com/webhook/scheduler";

    private WebClient capturing(AtomicReference<ClientRequest> captured, AtomicInteger calls, HttpStatus status) {
        return WebClient.builder()
                .exchangeFunction(request -> {
                    captured.set(request);
                    calls.incrementAndGet();
                    return Mono.just(ClientResponse.create(status).build());
                })
                .build();
    }

    @Test
    void 웹훅_URL이_비어_있으면_전송하지_않는다() {
        AtomicInteger calls = new AtomicInteger();
        SchedulerFailureSender sender = new SchedulerFailureSender(
                capturing(new AtomicReference<>(), calls, HttpStatus.OK), "", "");

        assertDoesNotThrow(() -> sender.notifyFailure("Job", new RuntimeException("x")));

        assertEquals(0, calls.get());
    }

    @Test
    void 실패_알림에_작업명과_예외_종류가_담기고_시크릿_헤더가_실린다() {
        AtomicReference<ClientRequest> captured = new AtomicReference<>();
        SchedulerFailureSender sender = new SchedulerFailureSender(
                capturing(captured, new AtomicInteger(), HttpStatus.OK), URL, "test-secret");

        sender.notifyFailure("CheerExpirationScheduler.deleteExpiredCheers", new IllegalStateException("민감한 내용"));

        assertEquals("test-secret", captured.get().headers().getFirst(SchedulerFailureSender.SECRET_HEADER));
        String message = SchedulerFailureSender.buildMessage(
                "CheerExpirationScheduler.deleteExpiredCheers", new IllegalStateException("민감한 내용"));
        assertTrue(message.contains("CheerExpirationScheduler.deleteExpiredCheers"));
        assertTrue(message.contains("IllegalStateException"));
        assertFalse(message.contains("민감한 내용"));
    }

    @Test
    void 시크릿이_비어_있으면_헤더를_붙이지_않는다() {
        AtomicReference<ClientRequest> captured = new AtomicReference<>();
        new SchedulerFailureSender(capturing(captured, new AtomicInteger(), HttpStatus.OK), URL, "")
                .notifyFailure("Job", new RuntimeException());

        assertNotNull(captured.get());
        assertFalse(captured.get().headers().containsKey(SchedulerFailureSender.SECRET_HEADER));
    }

    @Test
    void 이름을_모르면_스택의_앱_코드_첫_프레임을_발생_위치로_쓴다() {
        Throwable cause = new RuntimeException("boom");

        assertTrue(SchedulerFailureSender.locate(cause).startsWith("SchedulerFailureSenderTest."),
                SchedulerFailureSender.locate(cause));
    }

    @Test
    void 전송이_실패해도_예외를_던지지_않고_로그에_URL과_시크릿이_남지_않는다() {
        String secret = "super-secret-value";
        Logger logger = (Logger) LoggerFactory.getLogger(SchedulerFailureSender.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            SchedulerFailureSender forbidden = new SchedulerFailureSender(
                    capturing(new AtomicReference<>(), new AtomicInteger(), HttpStatus.FORBIDDEN), URL, secret);
            assertDoesNotThrow(() -> forbidden.notifyFailure("Job", new RuntimeException()));

            SchedulerFailureSender broken = new SchedulerFailureSender(
                    WebClient.builder().exchangeFunction(r -> Mono.error(new RuntimeException("POST " + URL + " failed"))).build(),
                    URL, secret);
            assertDoesNotThrow(() -> broken.notifyFailure("Job", new RuntimeException()));
        } finally {
            logger.detachAppender(appender);
        }

        assertEquals(2, appender.list.size());
        assertTrue(appender.list.get(0).getFormattedMessage().contains("403"));
        for (ILoggingEvent event : appender.list) {
            assertFalse(event.getFormattedMessage().contains("n8n.example.com"), event.getFormattedMessage());
            assertFalse(event.getFormattedMessage().contains(secret), event.getFormattedMessage());
        }
    }

    @Test
    void 스케줄러_에러_핸들러가_예외를_받으면_알림을_보낸다() {
        AtomicInteger calls = new AtomicInteger();
        SchedulerFailureSender sender = new SchedulerFailureSender(
                capturing(new AtomicReference<>(), calls, HttpStatus.OK), URL, "");
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        ThreadPoolTaskSchedulerCustomizer customizer = new SchedulerConfig().schedulerFailureAlertCustomizer(sender);
        customizer.customize(scheduler);
        scheduler.setPoolSize(1);
        scheduler.initialize();
        try {
            scheduler.schedule(() -> {
                throw new IllegalStateException("fail");
            }, java.time.Instant.now());
            long deadline = System.currentTimeMillis() + 3000;
            while (calls.get() == 0 && System.currentTimeMillis() < deadline) {
                Thread.onSpinWait();
            }
        } finally {
            scheduler.shutdown();
        }

        assertEquals(1, calls.get());
    }

    @Test
    void 스프링_컨텍스트에서_빈으로_생성되고_프로퍼티가_없어도_뜬다() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean("slackWebClient", WebClient.class, () -> WebClient.create());
            context.registerBean(SchedulerFailureSender.class);
            context.refresh();

            assertNotNull(context.getBean(SchedulerFailureSender.class));
        }
    }

    @Test
    void 본문에_text와_구조화_필드가_담기고_예외_메시지는_없다() {
        java.util.Map<String, Object> body = SchedulerFailureSender.body("메시지",
                SchedulerFailureSender.buildData("Job.run", new IllegalStateException("민감한 내용")));

        assertEquals("메시지", body.get("text"));
        assertEquals("Job.run", body.get("job"));
        assertEquals("IllegalStateException", body.get("exception"));
        assertTrue(body.containsKey("occurredAt"));
        assertFalse(body.toString().contains("민감한 내용"));
    }
}
