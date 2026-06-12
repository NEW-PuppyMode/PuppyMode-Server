package com.umc.puppymode2.domain.notification.scheduler;

import com.umc.puppymode2.domain.notification.dto.DrinkReminderTarget;
import com.umc.puppymode2.domain.notification.repository.FcmTokenRepository;
import com.umc.puppymode2.domain.notification.service.FcmSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DrinkReminderScheduler {

    private final FcmTokenRepository fcmTokenRepository;
    private final FcmSender fcmSender;

    // TODO: 멀티 인스턴스 배포 시 ShedLock 적용 필요
    @Scheduled(cron = "0 0 22 * * *", zone = "Asia/Seoul")
    public void sendDrinkReminder() {
        try {
            LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
            log.info("[DrinkReminder] 스케줄러 시작 - {}", today);

            List<DrinkReminderTarget> targets = fcmTokenRepository.findTargetsForDrinkReminder(today);
            log.info("[DrinkReminder] 발송 대상: {}명", targets.size());

            if (targets.isEmpty()) {
                log.info("[DrinkReminder] 발송 대상 없음 - 종료");
                return;
            }

            fcmSender.sendPersonalized(targets);
            log.info("[DrinkReminder] 발송 완료");
        } catch (Exception e) {
            log.error("[DrinkReminder] 예외 발생", e);
        }
    }
}