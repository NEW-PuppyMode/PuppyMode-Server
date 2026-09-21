package com.umc.puppymode2.domain.cheer.scheduler;

import com.umc.puppymode2.domain.cheer.repository.CheerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheerExpirationSchedulerTest {

    @Mock
    private CheerRepository cheerRepository;

    @InjectMocks
    private CheerExpirationScheduler scheduler;

    @Test
    void 만료_시각으로부터_1일이_더_지난_응원을_삭제한다() {
        when(cheerRepository.deleteAllExpiredBefore(org.mockito.ArgumentMatchers.any())).thenReturn(3);
        LocalDateTime before = LocalDateTime.now();

        scheduler.deleteExpiredCheers();

        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(cheerRepository).deleteAllExpiredBefore(captor.capture());
        // 기준 시각 = 지금 - 1일 (expires_at과 같은 JVM 기본 타임존 기준)
        Duration diff = Duration.between(captor.getValue(), before.minusDays(1)).abs();
        assertTrue(diff.getSeconds() < 5, "기준 시각이 지금-1일과 크게 다릅니다: " + captor.getValue());
    }

    @Test
    void 삭제할_응원이_없어도_정상_종료한다() {
        when(cheerRepository.deleteAllExpiredBefore(org.mockito.ArgumentMatchers.any())).thenReturn(0);

        scheduler.deleteExpiredCheers();

        verify(cheerRepository).deleteAllExpiredBefore(org.mockito.ArgumentMatchers.any());
    }
}
