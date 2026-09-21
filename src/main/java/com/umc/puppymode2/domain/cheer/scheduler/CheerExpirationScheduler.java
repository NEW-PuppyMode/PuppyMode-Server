package com.umc.puppymode2.domain.cheer.scheduler;

import com.umc.puppymode2.domain.cheer.repository.CheerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 만료된 응원을 정리하는 배치. 매일 00:10 KST에 실행한다.
 *
 * 받은 응원 목록은 조회 시점에 expires_at으로 이미 걸러서 보여주므로, 이 배치는 화면 표시와 무관하게
 * 테이블이 계속 커지는 것만 막는다.
 *
 * 만료 시각보다 1일 더 지난 행만 지운다. 응원을 보낸 쪽의 「응원 보냄」 판정이 어제/오늘 날짜의 응원 행을
 * 필요로 하는데, 그 행의 만료 시각은 항상 그보다 뒤라서 지워지지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CheerExpirationScheduler {

    // 만료 시각으로부터 이만큼 더 지난 응원을 삭제 대상으로 본다.
    private static final long RETENTION_DAYS_AFTER_EXPIRY = 1;

    private final CheerRepository cheerRepository;

    // TODO: 멀티 인스턴스 배포 시 중복 실행 방지를 위해 ShedLock 적용 필요 (기존 스케줄러와 동일한 과제)
    //       다만 삭제는 같은 조건을 두 번 실행해도 결과가 같아서(멱등) 중복 실행되어도 데이터가 어긋나지는 않는다.
    @Scheduled(cron = "0 10 0 * * *", zone = "Asia/Seoul")
    @Transactional
    public void deleteExpiredCheers() {
        // expires_at은 JVM 기본 타임존 기준으로 저장되므로 기준 시각도 같은 기준(LocalDateTime.now())으로 만든다.
        LocalDateTime cutoff = LocalDateTime.now().minusDays(RETENTION_DAYS_AFTER_EXPIRY);
        int deleted = cheerRepository.deleteAllExpiredBefore(cutoff);
        log.info("[CHEER EXPIRATION] 만료 응원 {}건 삭제 (기준 시각: {})", deleted, cutoff);
    }
}
