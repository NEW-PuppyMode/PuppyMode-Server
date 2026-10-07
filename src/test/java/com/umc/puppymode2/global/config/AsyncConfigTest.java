package com.umc.puppymode2.global.config;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsyncConfigTest {

    @Test
    void 신고_알림_실행기는_가득_차도_예외를_던지지_않고_작업을_버린다() throws Exception {
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) new AsyncConfig().complaintExecutor();
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger ran = new AtomicInteger();
        CountDownLatch done = new CountDownLatch(22);
        Runnable blocking = () -> {
            try {
                release.await();
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            ran.incrementAndGet();
            done.countDown();
        };

        try {
            try {
                // 최대 스레드 2개 + 큐 20개 = 22개까지 받는다.
                for (int i = 0; i < 22; i++) {
                    executor.execute(blocking);
                }
                // 23번째부터는 거부되지만 예외 없이 버려져야 한다. (기본 정책이면 TaskRejectedException)
                assertDoesNotThrow(() -> executor.execute(blocking));
                assertDoesNotThrow(() -> executor.execute(blocking));
            } finally {
                release.countDown(); // 중간에 실패해도 붙잡힌 스레드가 풀려나도록 항상 푼다
            }
            // 받아들인 22개는 모두 실행되고, 버려진 작업은 실행되지 않는다.
            assertTrue(done.await(5, TimeUnit.SECONDS));
            assertEquals(22, ran.get());
        } finally {
            // 검증이 실패해도 non-daemon 스레드가 남아 테스트 프로세스를 붙잡지 않도록 반드시 종료한다.
            // (shutdown은 대기 중인 작업을 버리므로 위에서 작업이 끝난 걸 확인한 뒤에 호출한다)
            executor.shutdown();
        }
    }

    @Test
    void 푸시_알림_실행기와_신고_알림_실행기는_별도_풀이다() {
        AsyncConfig config = new AsyncConfig();
        ThreadPoolTaskExecutor notification = (ThreadPoolTaskExecutor) config.notificationExecutor();
        ThreadPoolTaskExecutor complaint = (ThreadPoolTaskExecutor) config.complaintExecutor();
        try {
            assertNotSame(notification, complaint);
        } finally {
            notification.shutdown();
            complaint.shutdown();
        }
    }
}
