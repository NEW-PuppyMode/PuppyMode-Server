package com.umc.puppymode2.domain.user.batch;

import com.umc.puppymode2.domain.user.repository.WithDrawnUserArchiveRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class WithdrawalDataCleanupBatch {

    private final WithDrawnUserArchiveRepository withdrawnUserArchiveRepository;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupExpiredWithdrawalData() {
        try {
            withdrawnUserArchiveRepository.deleteExpiredArchive(LocalDateTime.now());
            log.info("[Batch] 만료된 탈퇴 보관 데이터 삭제 완료");
        } catch (Exception e) {
            log.error("[Batch] 탈퇴 데이터 삭제 실패", e);
        }
    }
}
