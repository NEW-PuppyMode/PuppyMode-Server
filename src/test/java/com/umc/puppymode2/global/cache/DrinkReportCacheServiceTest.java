package com.umc.puppymode2.global.cache;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

import java.time.YearMonth;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DrinkReportCacheServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @InjectMocks
    private DrinkReportCacheService cacheService;

    private final Long userId = 1L;
    private final YearMonth month = YearMonth.of(2025, 8);
    private final String key = "report:1:2025-08";

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
}
