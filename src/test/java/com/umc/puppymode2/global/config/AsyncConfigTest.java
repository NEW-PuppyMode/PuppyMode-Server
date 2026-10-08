package com.umc.puppymode2.global.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;

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

    private static ObjectProvider<MeterRegistry> providerOf(MeterRegistry registry) {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton("meterRegistry", registry);
        return beanFactory.getBeanProvider(MeterRegistry.class);
    }

    private static double dropped(MeterRegistry registry) {
        return registry.counter(AsyncConfig.DROPPED_METRIC, "name", "complaintExecutor").count();
    }

    @Test
    void 폐기된_알림마다_카운터가_늘고_첫_폐기_전에도_0으로_등록되어_있다() throws Exception {
        MeterRegistry registry = new SimpleMeterRegistry();
        AsyncConfig config = new AsyncConfig();
        ReflectionTestUtils.setField(config, "meterRegistry", providerOf(registry));
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) config.complaintExecutor();
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(22);
        Runnable blocking = () -> {
            try {
                release.await();
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            done.countDown();
        };

        try {
            // 아직 아무것도 버려지지 않았지만 시계열은 이미 있어야 한다. (없으면 알람 규칙이 "데이터 없음"이 된다)
            assertEquals(0.0, registry.find(AsyncConfig.DROPPED_METRIC).tag("name", "complaintExecutor").counter().count());
            try {
                for (int i = 0; i < 22; i++) {
                    executor.execute(blocking);
                }
                assertEquals(0.0, dropped(registry), "받아들인 22건은 폐기로 세면 안 된다");
                for (int i = 0; i < 3; i++) {
                    executor.execute(blocking); // 23~25번째는 거부되어 폐기된다
                }
            } finally {
                release.countDown();
            }
            assertTrue(done.await(5, TimeUnit.SECONDS));
            assertEquals(3.0, dropped(registry));
        } finally {
            executor.shutdown();
        }
    }

    @Test
    void 레지스트리가_없어도_폐기_경로는_예외를_던지지_않는다() {
        AsyncConfig config = new AsyncConfig();
        ObjectProvider<MeterRegistry> empty = new DefaultListableBeanFactory().getBeanProvider(MeterRegistry.class); // 레지스트리 빈이 없는 경우
        ReflectionTestUtils.setField(config, "meterRegistry", empty);
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) config.complaintExecutor();
        CountDownLatch release = new CountDownLatch(1);
        Runnable blocking = () -> {
            try {
                release.await();
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        };
        try {
            for (int i = 0; i < 22; i++) {
                executor.execute(blocking);
            }
            assertDoesNotThrow(() -> executor.execute(blocking));
        } finally {
            release.countDown();
            executor.shutdown();
        }
    }
}
