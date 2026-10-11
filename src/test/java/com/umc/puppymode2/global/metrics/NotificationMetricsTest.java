package com.umc.puppymode2.global.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class NotificationMetricsTest {

    private static ObjectProvider<MeterRegistry> providerOf(MeterRegistry registry) {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        if (registry != null) {
            beanFactory.registerSingleton("meterRegistry", registry);
        }
        return beanFactory.getBeanProvider(MeterRegistry.class);
    }

    @Test
    void 폐기_카운터는_name과_reason_태그로_세고_미리_등록하면_0으로_보인다() {
        MeterRegistry registry = new SimpleMeterRegistry();
        NotificationMetrics metrics = new NotificationMetrics(providerOf(registry));

        metrics.registerDropped("notificationExecutor", "queue_full");
        assertEquals(0.0, registry.get(NotificationMetrics.DROPPED).tag("name", "notificationExecutor").tag("reason", "queue_full").counter().count());

        metrics.countDropped("notificationExecutor", "queue_full");
        metrics.countDropped("notificationExecutor", "queue_full");
        assertEquals(2.0, registry.get(NotificationMetrics.DROPPED).tag("name", "notificationExecutor").tag("reason", "queue_full").counter().count());
    }

    @Test
    void 전송_시간은_name과_result_태그로_기록한다() {
        MeterRegistry registry = new SimpleMeterRegistry();
        NotificationMetrics metrics = new NotificationMetrics(providerOf(registry));

        metrics.recordSend("push", "success", Duration.ofMillis(300));
        metrics.recordSend("push", "failure", Duration.ofMillis(10));

        assertEquals(1, registry.get(NotificationMetrics.SEND).tag("name", "push").tag("result", "success").timer().count());
        assertEquals(1, registry.get(NotificationMetrics.SEND).tag("name", "push").tag("result", "failure").timer().count());
    }

    @Test
    void 레지스트리가_없거나_수동_생성이어도_예외를_던지지_않는다() {
        assertDoesNotThrow(() -> NotificationMetrics.noop().countDropped("x", "queue_full"));
        assertDoesNotThrow(() -> NotificationMetrics.noop().recordSend("push", "success", Duration.ofMillis(1)));
        assertDoesNotThrow(() -> new NotificationMetrics(providerOf(null)).countDropped("x", "queue_full"));
    }
}
