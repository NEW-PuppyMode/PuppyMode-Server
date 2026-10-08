package com.umc.puppymode2.domain.notification.service;

import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FakeFcmClientTest {

    private static final String LOCAL_DB = "jdbc:mysql://localhost:3306/puppymode2db?serverTimezone=Asia/Seoul";

    private final MeterRegistry registry = new SimpleMeterRegistry();

    private static List<Message> messages(int n) {
        List<Message> list = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            list.add(Message.builder().setToken("fake-token-" + i).putData("k", "v").build());
        }
        return list;
    }

    private FakeFcmClient client(long delayMs, int slowPct, long slowDelayMs, int failurePct, String controlFile) {
        return new FakeFcmClient(registry, LOCAL_DB, delayMs, slowPct, slowDelayMs, failurePct, "UNAVAILABLE", controlFile);
    }

    @Test
    void 설정한_지연만큼_걸리고_호출_토큰_건수를_센다() throws Exception {
        FakeFcmClient client = client(120, 0, 0, 0, "");

        long start = System.nanoTime();
        FcmClient.BatchResult result = client.sendEach(messages(2));
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertTrue(elapsedMs >= 110, "지연이 적용되어야 한다: " + elapsedMs + "ms");
        assertEquals(2, result.successCount());
        assertEquals(0, result.failureCount());
        assertEquals(1.0, registry.get("fake_fcm_calls").counter().count());
        assertEquals(2.0, registry.get("fake_fcm_tokens").counter().count());
        assertEquals(0.0, registry.get("fake_fcm_inflight").gauge().value());
    }

    @Test
    void 실패_비율만큼_토큰을_실패시키고_일시_오류_코드를_돌려준다() throws Exception {
        FakeFcmClient client = client(0, 0, 0, 30, "");

        FcmClient.BatchResult result = client.sendEach(messages(100));

        assertEquals(30, result.failureCount());
        assertEquals(70, result.successCount());
        assertEquals(30.0, registry.get("fake_fcm_failed_tokens").counter().count());
        FcmClient.Item failed = result.items().stream().filter(i -> !i.successful()).findFirst().orElseThrow();
        assertEquals(MessagingErrorCode.UNAVAILABLE, failed.errorCode());
        // 토큰 삭제를 일으키는 코드(UNREGISTERED, INVALID_ARGUMENT)가 아니어야 부하 중에 테스트 데이터가 지워지지 않는다.
        assertNotEquals(MessagingErrorCode.UNREGISTERED, failed.errorCode());
        assertNotEquals(MessagingErrorCode.INVALID_ARGUMENT, failed.errorCode());
    }

    @Test
    void 느린_응답_비율은_호출_순번_기준으로_정해진다() throws Exception {
        FakeFcmClient client = client(0, 5, 40, 0, ""); // 100번 중 5번만 40ms

        long start = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            client.sendEach(messages(1));
        }
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertTrue(elapsedMs >= 5 * 40 - 10, "느린 호출 5번의 지연이 합쳐져야 한다: " + elapsedMs + "ms");
        assertTrue(elapsedMs < 100 * 40 / 2, "나머지 95번은 지연이 없어야 한다: " + elapsedMs + "ms");
    }

    @Test
    void 제어_파일을_고치면_재시작_없이_지연이_바뀐다(@TempDir Path dir) throws Exception {
        File control = dir.resolve("control.json").toFile();
        Files.writeString(control.toPath(), "{\"delayMs\": 0}");
        FakeFcmClient client = client(0, 0, 0, 0, control.getAbsolutePath());

        long start = System.nanoTime();
        client.sendEach(messages(1));
        assertTrue((System.nanoTime() - start) / 1_000_000 < 100);

        Files.writeString(control.toPath(), "{\"delayMs\": 150}");
        assertTrue(control.setLastModified(control.lastModified() + 2000)); // 변경 시각이 같은 초라 못 알아채는 일을 막는다
        start = System.nanoTime();
        client.sendEach(messages(1));
        assertTrue((System.nanoTime() - start) / 1_000_000 >= 140, "바뀐 지연이 적용되어야 한다");
    }

    @Test
    void 제어_파일이_깨져도_이전_설정을_유지하고_예외를_던지지_않는다(@TempDir Path dir) throws Exception {
        File control = dir.resolve("control.json").toFile();
        Files.writeString(control.toPath(), "{not json");
        FakeFcmClient client = client(0, 0, 0, 0, control.getAbsolutePath());

        assertDoesNotThrow(() -> client.sendEach(messages(1)));
    }

    @Test
    void localhost가_아닌_DB에서는_만들어지지_않는다() {
        assertThrows(IllegalStateException.class, () ->
                new FakeFcmClient(registry, "jdbc:mysql://prod-db.example.ap-northeast-2.rds.amazonaws.com:3306/puppymode",
                        0, 0, 0, 0, "UNAVAILABLE", ""));
        assertThrows(IllegalStateException.class, () ->
                new FakeFcmClient(registry, "jdbc:mysql://localhost.evil.example:3306/db", 0, 0, 0, 0, "UNAVAILABLE", ""));
    }

    @Test
    void 프로퍼티가_없으면_실제_구현만_등록되고_true일_때만_가짜가_등록된다() {
        ApplicationContextRunner runner = new ApplicationContextRunner()
                .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                .withUserConfiguration(FirebaseFcmClient.class, FakeFcmClient.class);

        runner.withPropertyValues("spring.datasource.url=" + LOCAL_DB).run(context -> {
            assertEquals(1, context.getBeansOfType(FcmClient.class).size());
            assertInstanceOf(FirebaseFcmClient.class, context.getBean(FcmClient.class));
        });
        runner.withPropertyValues("spring.datasource.url=" + LOCAL_DB, "loadtest.fake-fcm.enabled=false").run(context ->
                assertInstanceOf(FirebaseFcmClient.class, context.getBean(FcmClient.class)));
        runner.withPropertyValues("spring.datasource.url=" + LOCAL_DB, "loadtest.fake-fcm.enabled=true").run(context -> {
            assertEquals(1, context.getBeansOfType(FcmClient.class).size());
            assertInstanceOf(FakeFcmClient.class, context.getBean(FcmClient.class));
        });
    }

    @Test
    void 가짜를_켜도_원격_DB면_앱_기동이_실패한다() {
        new ApplicationContextRunner()
                .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                .withUserConfiguration(FirebaseFcmClient.class, FakeFcmClient.class)
                .withPropertyValues("spring.datasource.url=jdbc:mysql://prod-db.example.com:3306/puppymode",
                        "loadtest.fake-fcm.enabled=true")
                .run(context -> assertNotNull(context.getStartupFailure()));
    }
}
