package com.umc.puppymode2.global.cache;

import com.umc.puppymode2.domain.report.dto.DrinkReportResponseDTO;
import com.umc.puppymode2.global.util.TimeConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.YearMonth;
import java.time.ZonedDateTime;

/**
 * 월간 음주 리포트(DrinkReportResponseDTO)에 대한 cache-aside 캐시.
 *
 * 리포트는 goal(목표), drinkHistory(음주 기록), advice(잔소리 기록) 세 도메인의 데이터를
 * 조합해 매번 다시 계산하므로, 세 도메인 중 하나라도 쓰기가 발생하면 해당 (userId, 월) 캐시를
 * 즉시 evict해야 한다. TTL은 그 evict를 놓쳤을 때를 대비한 최후의 안전장치이다.
 *
 * Redis 장애 시에는 캐시를 건너뛰고 DB 조회로 폴백한다(리포트 조회는 Redis 없이도 동작해야 함).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DrinkReportCacheService {

    // 캐시에 저장되는 DrinkReportResponseDTO의 필드 구성이 바뀌면 이 버전을 올린다.
    // 배포 직후 기존 키(구 버전 JSON)는 아무도 읽지 않게 되고 각자 TTL이 지나면 사라지며,
    // 새 키로는 첫 요청부터 새 필드가 채워진 값이 재계산되어 저장된다.
    // (v2: goalStatus 필드 추가 - #184)
    private static final String KEY_PREFIX = "report:v2:";
    private static final Duration CURRENT_MONTH_TTL = Duration.ofMinutes(5);
    private static final Duration PAST_MONTH_TTL = Duration.ofDays(1);

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 성능 측정 baseline용 스위치.
     * false로 두면 캐시를 완전히 우회해 매 요청이 DB로 직행한다.
     * (같은 빌드에서 -Dreport.cache.enabled=false 또는
     *  REPORT_CACHE_ENABLED=false 로 전/후 비교 가능)
     */
    @Value("${report.cache.enabled:true}")
    private boolean cacheEnabled = true;

    public DrinkReportResponseDTO get(Long userId, YearMonth targetMonth) {
        if (!cacheEnabled) {
            return null;
        }
        try {
            Object cached = redisTemplate.opsForValue().get(buildKey(userId, targetMonth));
            if (cached instanceof DrinkReportResponseDTO dto) {
                return dto;
            }
            return null;
        } catch (Exception e) {
            log.warn("[REPORT CACHE] 조회 실패, DB로 폴백합니다: {}", e.getMessage());
            return null;
        }
    }

    public void put(Long userId, YearMonth targetMonth, DrinkReportResponseDTO dto) {
        if (!cacheEnabled) {
            return;
        }
        try {
            // 서버 기본 타임존이 아니라 KST 기준으로 "이번 달"을 판단한다.
            // 그렇지 않으면 매월 1일 새벽(KST 00:00~08:59) 동안 이번 달 리포트가
            // 과거 달로 오인되어 TTL이 5분이 아니라 1일로 잘못 잡힌다 (#168과 동일 원인).
            Duration ttl;
            if (targetMonth.equals(YearMonth.now(TimeConstants.KST))) {
                // 이번 달 리포트는 최대 5분 캐시하되, 월 경계를 넘겨 살아남게 두지 않는다.
                // 예: KST 8/31 23:59에 계산한 8월 리포트를 그대로 5분 캐시하면,
                // 9/1 00:00 직후 몇 분간 goalStatus=IN_PROGRESS 처럼 "이번 달" 기준으로 계산된 값이
                // 이미 과거가 된 8월 조회에 잘못 반환된다(achievementRate도 동일 문제).
                // 다음 달 시작(KST 00:00)까지 남은 시간을 TTL 상한으로 건다.
                Duration untilNextMonth = durationUntilNextMonthStartKst();
                if (untilNextMonth.isZero() || untilNextMonth.isNegative()) {
                    // YearMonth.now() 판정 직후 월 경계를 넘어선 극단적 타이밍.
                    // 이번 달로 캐시할 이유가 없으니 저장을 건너뛴다(다음 요청이 과거 달로 다시 캐시).
                    return;
                }
                ttl = CURRENT_MONTH_TTL.compareTo(untilNextMonth) <= 0 ? CURRENT_MONTH_TTL : untilNextMonth;
            } else {
                ttl = PAST_MONTH_TTL;
            }
            redisTemplate.opsForValue().set(buildKey(userId, targetMonth), dto, ttl);
        } catch (Exception e) {
            log.warn("[REPORT CACHE] 저장 실패, 캐싱 없이 진행합니다: {}", e.getMessage());
        }
    }

    /**
     * 지금(KST)부터 다음 달 1일 00:00(KST)까지 남은 시간.
     * 이번 달 리포트 캐시가 월 경계를 넘겨 유효하지 않은 상태로 반환되는 것을 막기 위한 TTL 상한.
     */
    private Duration durationUntilNextMonthStartKst() {
        ZonedDateTime nowKst = ZonedDateTime.now(TimeConstants.KST);
        ZonedDateTime nextMonthStart = nowKst.toLocalDate()
                .withDayOfMonth(1)
                .plusMonths(1)
                .atStartOfDay(TimeConstants.KST);
        return Duration.between(nowKst, nextMonthStart);
    }

    /**
     * (userId, 월) 캐시를 무효화한다.
     *
     * 호출 시점에 활성 트랜잭션이 있으면, 그 트랜잭션이 커밋된 이후에 실제 삭제를 수행하도록 미룬다.
     * 커밋 전에 바로 지우면, 그 사이에 끼어든 다른 요청이 아직 커밋되지 않은(=변경 전) 데이터로
     * 캐시를 다시 채워버릴 수 있고, 그 stale 값이 TTL(최대 1일)만큼 남아있게 된다.
     * 활성 트랜잭션이 없으면(예: 트랜잭션 밖에서 호출) 지금처럼 즉시 삭제한다.
     */
    public void evict(Long userId, YearMonth targetMonth) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    doEvict(userId, targetMonth);
                }
            });
        } else {
            doEvict(userId, targetMonth);
        }
    }

    private void doEvict(Long userId, YearMonth targetMonth) {
        try {
            redisTemplate.delete(buildKey(userId, targetMonth));
        } catch (Exception e) {
            log.warn("[REPORT CACHE] 무효화 실패: {}", e.getMessage());
        }
    }

    private String buildKey(Long userId, YearMonth targetMonth) {
        return KEY_PREFIX + userId + ":" + targetMonth;
    }
}
