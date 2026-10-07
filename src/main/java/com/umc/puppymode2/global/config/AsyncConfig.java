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
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) ->
                log.error("[비동기 처리 실패] method={}", method.getName(), ex);
    }
}
