package com.umc.puppymode2.global.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.function.Consumer;

/**
 * 알림 경로의 메트릭. Prometheus에서는 아래 이름으로 보인다.
 * - notification_dropped_total{name, reason}  폐기된 알림 수 (reason: queue_full 등)
 * - notification_send_seconds{name, result}   전송 시간 (name: push 등, result: success / failure / error)
 *
 * 같은 이름의 지표는 태그 키가 모두 같아야 Prometheus에 등록된다. 그래서 태그 키는 이 클래스에서만 정한다.
 * 메트릭은 부가 기능이다. 레지스트리가 없거나 기록이 실패해도 알림 경로에 예외를 던지지 않는다.
 */
@Component
public class NotificationMetrics {

    public static final String DROPPED = "notification.dropped";
    public static final String SEND = "notification.send";

    private final ObjectProvider<MeterRegistry> registry;

    public NotificationMetrics(ObjectProvider<MeterRegistry> registry) {
        this.registry = registry;
    }

    /** 레지스트리 없이 만드는 테스트·수동 생성용. 모든 기록을 건너뛴다. */
    public static NotificationMetrics noop() {
        return new NotificationMetrics(null);
    }

    /** 첫 폐기 전에도 시계열이 있도록 0으로 미리 등록한다. (없으면 알람 규칙이 "데이터 없음"이 된다) */
    public void registerDropped(String name, String reason) {
        withRegistry(r -> dropped(r, name, reason));
    }

    public void countDropped(String name, String reason) {
        withRegistry(r -> dropped(r, name, reason).increment());
    }

    public void recordSend(String name, String result, Duration elapsed) {
        withRegistry(r -> Timer.builder(SEND)
                .tag("name", name)
                .tag("result", result)
                .description("알림 전송 시간")
                .publishPercentileHistogram()
                .register(r)
                .record(elapsed));
    }

    private static Counter dropped(MeterRegistry r, String name, String reason) {
        return Counter.builder(DROPPED)
                .tag("name", name)
                .tag("reason", reason)
                .description("폐기된 알림 수")
                .register(r);
    }

    private void withRegistry(Consumer<MeterRegistry> action) {
        try {
            MeterRegistry r = registry == null ? null : registry.getIfAvailable();
            if (r != null) {
                action.accept(r);
            }
        } catch (RuntimeException ignored) {
            // 메트릭은 부가 기능이다.
        }
    }
}
