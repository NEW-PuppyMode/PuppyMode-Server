package com.umc.puppymode2.global.cache;

import com.umc.puppymode2.domain.report.dto.DrinkReportResponseDTO;
import com.umc.puppymode2.global.util.TimeConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

import java.time.Duration;
import java.time.YearMonth;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DrinkReportCacheServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private DrinkReportCacheService cacheService;

    private final Long userId = 1L;
    private final YearMonth month = YearMonth.of(2025, 8);
    private final String key = "report:1:2025-08";

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void 활성_트랜잭션이_없으면_evict가_즉시_실행된다() {
        cacheService.evict(userId, month);

        verify(redisTemplate).delete(key);
    }

    @Test
    void 트랜잭션이_커밋되면_evict가_실행된다() {
        TransactionSynchronizationManager.initSynchronization();

        cacheService.evict(userId, month);

        // 커밋 전이므로 아직 삭제되면 안 됨
        verify(redisTemplate, never()).delete(key);

        TransactionSynchronizationUtils.triggerAfterCommit();

        verify(redisTemplate).delete(key);
    }

    @Test
    void 트랜잭션이_롤백되면_evict가_실행되지_않는다() {
        TransactionSynchronizationManager.initSynchronization();

        cacheService.evict(userId, month);

        // afterCommit 없이 afterCompletion(ROLLED_BACK)만 호출되는 롤백 상황을 흉내낸다
        TransactionSynchronizationUtils.triggerAfterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

        verify(redisTemplate, never()).delete(key);
    }

    /**
     * #168과 동일한 원인의 회귀 테스트.
     *
     * put()의 TTL 분기가 YearMonth.now()를 타임존 없이 호출하면, 서버 JVM 기본 타임존이 UTC일 때
     * 매월 1일 새벽(KST 00:00~08:59)에 "이번 달"이 "과거 달"로 오인되어 TTL이 5분이 아니라
     * 1일로 잘못 잡힌다. 실제 벽시계 시각에 의존하면 하루 중 특정 시간대에만 재현되는
     * flaky한 테스트가 되므로, YearMonth.now(TimeConstants.KST) 호출 자체를 static mock으로
     * 가로채 결정론적으로 검증한다.
     */
    @Test
    void 이번달이면_TTL_5분이_적용된다() {
        DrinkReportResponseDTO dto = mock(DrinkReportResponseDTO.class);

        try (MockedStatic<YearMonth> mockedYearMonth = mockStatic(YearMonth.class, CALLS_REAL_METHODS)) {
            mockedYearMonth.when(() -> YearMonth.now(TimeConstants.KST)).thenReturn(month);

            cacheService.put(userId, month, dto);

            mockedYearMonth.verify(() -> YearMonth.now(TimeConstants.KST), atLeastOnce());
            mockedYearMonth.verify(YearMonth::now, never());
        }

        verify(valueOperations).set(eq(key), eq(dto), eq(Duration.ofMinutes(5)));
    }

    @Test
    void 과거달이면_TTL_1일이_적용된다() {
        YearMonth currentMonth = YearMonth.of(2025, 9); // "지금"은 9월이라고 가정
        DrinkReportResponseDTO dto = mock(DrinkReportResponseDTO.class);

        try (MockedStatic<YearMonth> mockedYearMonth = mockStatic(YearMonth.class, CALLS_REAL_METHODS)) {
            mockedYearMonth.when(() -> YearMonth.now(TimeConstants.KST)).thenReturn(currentMonth);

            cacheService.put(userId, month, dto); // month = 2025-08, 즉 과거 달

            mockedYearMonth.verify(() -> YearMonth.now(TimeConstants.KST), atLeastOnce());
        }

        verify(valueOperations).set(eq(key), eq(dto), eq(Duration.ofDays(1)));
    }
}
