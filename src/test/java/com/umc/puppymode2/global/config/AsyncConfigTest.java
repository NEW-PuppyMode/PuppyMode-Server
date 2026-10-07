package com.umc.puppymode2.global.config;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
            // 최대 스레드 2개 + 큐 20개 = 22개까지 받는다.
            for (int i = 0; i < 22; i++) {
                executor.execute(blocking);
            }
            // 23번째부터는 거부되지만 예외 없이 버려져야 한다. (기본 정책이면 TaskRejectedException)
            assertDoesNotThrow(() -> executor.execute(blocking));
            assertDoesNotThrow(() -> executor.execute(blocking));
        } finally {
            release.countDown();
        }
        // 받아들인 22개는 모두 실행되고, 버려진 작업은 실행되지 않는다. (shutdown은 대기 중인 작업을 버리므로 끝난 뒤에 한다)
        assertEquals(true, done.await(5, java.util.concurrent.TimeUnit.SECONDS));
        executor.shutdown();
        assertEquals(22, ran.get());
    }

    @Test
    void 푸시_알림_실행기와_신고_알림_실행기는_별도_풀이다() {
        AsyncConfig config = new AsyncConfig();
        Executor notification = config.notificationExecutor();
        Executor complaint = config.complaintExecutor();

        assertEquals(false, notification == complaint);
        ((ThreadPoolTaskExecutor) notification).shutdown();
        ((ThreadPoolTaskExecutor) complaint).shutdown();
    }
}
