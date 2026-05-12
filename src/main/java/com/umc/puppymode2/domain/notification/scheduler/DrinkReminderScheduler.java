package com.umc.puppymode2.domain.notification.scheduler;

import com.umc.puppymode2.domain.notification.dto.DrinkReminderTarget;
import com.umc.puppymode2.domain.notification.repository.FcmTokenRepository;
import com.umc.puppymode2.domain.notification.service.FcmSender;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DrinkReminderScheduler {

    private final FcmTokenRepository fcmTokenRepository;
    private final FcmSender fcmSender;

    // TODO: 멀티 인스턴스 배포 시 ShedLock 적용 필요
    @Scheduled(cron = "0 0 22 * * *", zone = "Asia/Seoul")
    public void sendDrinkReminder() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        List<DrinkReminderTarget> targets = fcmTokenRepository.findTargetsForDrinkReminder(today);

        if (targets.isEmpty()) return;

        fcmSender.sendPersonalized(targets);
    }
}