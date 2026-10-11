package com.umc.puppymode2.global.config;

import com.umc.puppymode2.global.metrics.NotificationMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    // 큐 포화로 폐기·거부된 알림 수. Prometheus에서는 notification_dropped_total{name="...",reason="queue_full"} 로 보인다.
    static final String DROPPED_METRIC = NotificationMetrics.DROPPED;
    private static final String QUEUE_FULL = "queue_full";
    private static final String NOTIFICATION_EXECUTOR = "notificationExecutor";
    private static final String COMPLAINT_EXECUTOR = "complaintExecutor";

    @Autowired
    private ObjectProvider<MeterRegistry> meterRegistry;

    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("notify-");
        // 큐가 가득 차면 예외를 던지지 않고 경고 한 줄만 남긴 채 푸시를 버린다. 푸시는 부가 기능이고 응원 자체는 이미 커밋된 뒤다.
        // (기본 AbortPolicy면 스프링이 AFTER_COMMIT 리스너의 거부 예외를 ERROR 스택트레이스 약 178줄(26KB)로 남겨
        //  포화 때 로그가 폭증하고 정상 장애 알람과 구분되지 않았다. #216 부하 테스트) 폐기 건수는 메트릭으로 계속 센다.
        executor.setRejectedExecutionHandler((task, pool) -> {
            metrics().countDropped(NOTIFICATION_EXECUTOR, QUEUE_FULL);
            log.warn("[푸시 알림] 작업 큐가 가득 차 알림을 건너뜁니다. (active={}, queued={})",
                    pool.getActiveCount(), pool.getQueue().size());
        });
        metrics().registerDropped(NOTIFICATION_EXECUTOR, QUEUE_FULL); // 첫 거부 전에도 0으로 보이도록 미리 등록한다
        executor.initialize();
        return executor;
    }

    // 신고 접수 Slack 알림 전용. 역할을 분리하기 위해 푸시 알림(notificationExecutor)과 풀을 따로 둔다.
    // - 푸시 알림이 늘어나 notificationExecutor가 밀려도 신고 알림은 막히지 않는다.
    // - 외부(Slack) 호출이 느려져도 푸시 알림에 영향을 주지 않는다.
    // 신고는 드문 이벤트라 작게 잡았다.
    @Bean(name = "complaintExecutor")
    public Executor complaintExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("complaint-");
        // 큐가 가득 차면 예외를 던지지 않고 경고만 남긴 채 알림을 버린다. 알림은 부가 기능이고 신고 접수는 이미 커밋된 뒤다.
        // (스프링이 AFTER_COMMIT 리스너의 거부 예외를 삼키고 ERROR 스택트레이스로 남기므로, 의도를 명시하고 로그를 한 줄로 줄인다)
        executor.setRejectedExecutionHandler((task, pool) -> {
            metrics().countDropped(COMPLAINT_EXECUTOR, QUEUE_FULL);
            log.warn("[신고 알림] 작업 큐가 가득 차 알림을 건너뜁니다. (active={}, queued={})",
                    pool.getActiveCount(), pool.getQueue().size());
        });
        metrics().registerDropped(COMPLAINT_EXECUTOR, QUEUE_FULL); // 첫 폐기 전에도 0으로 보이도록 미리 등록한다
        executor.initialize();
        return executor;
    }

    // 레지스트리는 지연 조회한다. 설정 클래스를 직접 생성하는 테스트에서는 null이고, 이때는 카운터를 건너뛴다.
    // 폐기·거부 핸들러는 어떤 경우에도 메트릭 때문에 예외를 던지면 안 된다. (NotificationMetrics가 예외를 삼킨다)
    private NotificationMetrics metrics() {
        return new NotificationMetrics(meterRegistry);
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) ->
                log.error("[비동기 처리 실패] method={}", method.getName(), ex);
    }
}
