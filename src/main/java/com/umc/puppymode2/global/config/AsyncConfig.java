package com.umc.puppymode2.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
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

    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("notify-");
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
        executor.setRejectedExecutionHandler((task, pool) ->
                log.warn("[신고 알림] 작업 큐가 가득 차 알림을 건너뜁니다. (active={}, queued={})",
                        pool.getActiveCount(), pool.getQueue().size()));
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) ->
                log.error("[비동기 처리 실패] method={}", method.getName(), ex);
    }
}
