package com.umc.puppymode2.global.config;

import com.umc.puppymode2.global.alert.SchedulerFailureSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.task.ThreadPoolTaskSchedulerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class SchedulerConfig {

    // @Scheduled 메서드가 예외를 밖으로 던지면 여기로 온다. 로그는 기존처럼 남기고 알림을 더한다.
    // (예외를 스스로 잡는 스케줄러는 직접 SchedulerFailureSender를 호출한다)
    @Bean
    public ThreadPoolTaskSchedulerCustomizer schedulerFailureAlertCustomizer(SchedulerFailureSender sender) {
        return scheduler -> scheduler.setErrorHandler(t -> {
            log.error("[스케줄러] 실행 중 예외 발생", t);
            sender.notifyFailure(t);
        });
    }
}
