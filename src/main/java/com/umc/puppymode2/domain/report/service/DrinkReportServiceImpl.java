package com.umc.puppymode2.domain.report.service;

import com.umc.puppymode2.domain.drinkhistory.repository.DrinkHistoryRepository;
import com.umc.puppymode2.domain.goal.entity.UserGoalHistory;
import com.umc.puppymode2.domain.goal.repository.UserGoalHistoryRepository;
import com.umc.puppymode2.domain.advice.repository.AdviceRepository;
import com.umc.puppymode2.domain.report.converter.DrinkReportConverter;
import com.umc.puppymode2.domain.report.dto.DrinkReportResponseDTO;
import com.umc.puppymode2.global.cache.DrinkReportCacheService;
import com.umc.puppymode2.global.util.TimeConstants;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.special.Beta;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;

@Slf4j
@Service
@RequiredArgsConstructor
public class DrinkReportServiceImpl implements DrinkReportService {

    /**
     * 베이지안 사전분포 강도(Default)
     * 값이 클수록 초기 목표 페이스를 더 신뢰한다.
     */
    private static final double BETA0 = 7.0;

    private final UserGoalHistoryRepository userGoalHistoryRepository;
    private final DrinkHistoryRepository drinkHistoryRepository;
    private final AdviceRepository adviceRepository;
    private final DrinkReportConverter drinkReportConverter;
    private final DrinkReportCacheService reportCacheService;

    @Transactional(Transactional.TxType.SUPPORTS)
    @Override
    public DrinkReportResponseDTO drinkReport(Long userId, YearMonth targetMonth) {

        // 캐시 적용 전/후 응답속도를 비교 측정하기 위한 타이머 (성능 측정용, 기능 로직과 무관)
        long start = System.nanoTime();

        // 1) cache-aside 조회: Redis에 캐시된 리포트가 있으면 DB를 전혀 거치지 않고 바로 반환
        DrinkReportResponseDTO cached = reportCacheService.get(userId, targetMonth);
        if (cached != null) {
            log.debug("[REPORT PERF] cache HIT userId={} month={} elapsedMs={}",
                    userId, targetMonth, elapsedMs(start));
            return cached;
        }

        // 2) 캐시 미스: 이하는 기존과 동일하게 goal/drinkHistory/advice를 조합해 직접 계산

        LocalDate startDate = targetMonth.atDay(1);
        LocalDate endDate = targetMonth.atEndOfMonth();

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);


        /*
         * 조회 대상 월(targetMonth)에 설정된 목표를 조회한다.
         * 해당 월의 목표가 존재하면 그 목표를 사용하고,
         * 존재하지 않으면 기본 목표(15회)를 사용한다.
         */
        int goal = userGoalHistoryRepository
                .findByUserIdAndGoalMonth(userId, targetMonth.atDay(1))
                .map(UserGoalHistory::getMonthlyGoalCount)
                .orElse(15);

        // 술을 정말 마신 날 (isDrink = True)
        long drinkDays = drinkHistoryRepository
                .countByUserUserIdAndIsDrinkTrueAndDrinkDateBetween(
                        userId,
                        startDate,
                        endDate
                );
        // 사용자의 해당 월 기록 횟수 (true, false 모두)
        long drinkRecordCount = drinkHistoryRepository
                .countByUserUserIdAndDrinkDateBetween(
                        userId,
                        startDate,
                        endDate
                );

        /*
         * 목표 달성 확률 계산에 필요한 값
         *
         * currentDay:
         * - 현재 월 조회 시: 오늘 날짜
         * - 과거 월 조회 시: 해당 월 마지막 날
         * - 미래 월 조회 시: 1일 기준
         *
         * daysInMonth:
         * - 조회 대상 월의 실제 일수
         */
        int currentDay = resolveCurrentDay(targetMonth);
        int daysInMonth = targetMonth.lengthOfMonth();

        /**
         * 목표 달성 확률(%)
         * 현재 월은 베이지안(Gamma-Poisson) 기반으로 계산하며,
         * 과거 월은 목표 달성 여부에 따라 0 또는 100을 반환한다.
         */
        int achievementRate = (int) Math.round(
                calculateGoalSuccessProbability(
                        goal,
                        drinkDays,
                        currentDay,
                        daysInMonth
                ) * 100
        );

        int scoldedCount = (int) adviceRepository
                .countByUserUserIdAndAdvisedAtBetween(
                        userId,
                        startDateTime,
                        endDateTime
                );

        DrinkReportResponseDTO result = drinkReportConverter.toDto(
                goal,
                drinkRecordCount,
                drinkDays,
                achievementRate,
                scoldedCount
        );

        // 3) 다음 조회부터는 캐시로 응답할 수 있도록 계산 결과를 Redis에 적재
        reportCacheService.put(userId, targetMonth, result);

        log.debug("[REPORT PERF] cache MISS userId={} month={} elapsedMs={}",
                userId, targetMonth, elapsedMs(start));

        return result;
    }

    /**
     * 성능 측정용 헬퍼. 나노초 타임스탬프를 밀리초(소수점 포함) 경과시간으로 변환한다.
     */
    private double elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000.0;
    }

    /**
     * 목표 달성 확률 계산
     *
     * 베이지안 Gamma-Poisson 모델을 기반으로
     * 남은 기간 동안 목표 음주 횟수 이내로 유지할 확률을 계산한다.
     *
     * 계산식
     *
     * shape  = (goal / daysInMonth) × beta0 + consumed
     * rate   = beta0 + day
     * budget = goal - consumed
     * p      = rate / (rate + (daysInMonth - day))
     *
     * probability = RegularizedIncompleteBeta(shape, budget + 1, p)
     *
     * @param goal 목표 음주 횟수
     * @param consumed 현재까지 음주 횟수
     * @param day 현재 날짜(며칠째)
     * @param daysInMonth 해당 월 총 일수
     * @return 목표 달성 확률(0~1)
     */
    private double calculateGoalSuccessProbability(
            int goal,
            long consumed,
            int day,
            int daysInMonth
    ) {

        if (goal <= 0) {
            return 0.0;
        }

        long budget = goal - consumed;

        // 이미 목표를 초과했다면 성공 확률은 0
        if (budget < 0) {
            return 0.0;
        }

        double shape =
                ((double) goal / daysInMonth) * BETA0 + consumed;

        double rate = BETA0 + day;

        double p =
                rate / (rate + (daysInMonth - day));

        return Beta.regularizedBeta(
                p,
                shape,
                budget + 1
        );
    }

    private int resolveCurrentDay(YearMonth targetMonth) {
        // 서버 JVM 기본 타임존(운영은 보통 UTC)이 아니라 항상 KST 기준으로 "오늘"을 계산한다.
        // 그렇지 않으면 한국 시간 00:00~08:59 사이에 날짜/월 계산이 하루 어긋난다 (#168과 동일 원인).
        LocalDate today = LocalDate.now(TimeConstants.KST);
        YearMonth currentMonth = YearMonth.from(today);

        if (targetMonth.equals(currentMonth)) {
            return today.getDayOfMonth();
        }

        if (targetMonth.isBefore(currentMonth)) {
            return targetMonth.lengthOfMonth();
        }

        return 1;
    }
}