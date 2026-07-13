package com.umc.puppymode2.domain.report.service;

import com.umc.puppymode2.domain.drinkhistory.repository.DrinkHistoryRepository;
import com.umc.puppymode2.domain.goal.entity.UserGoalHistory;
import com.umc.puppymode2.domain.goal.repository.UserGoalHistoryRepository;
import com.umc.puppymode2.domain.advice.repository.AdviceRepository;
import com.umc.puppymode2.domain.report.converter.DrinkReportConverter;
import com.umc.puppymode2.domain.report.dto.DrinkReportResponseDTO;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.math3.special.Beta;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;

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

    @Transactional(Transactional.TxType.SUPPORTS)
    @Override
    public DrinkReportResponseDTO drinkReport(Long userId, YearMonth targetMonth) {

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

        return drinkReportConverter.toDto(
                goal,
                drinkRecordCount,
                drinkDays,
                achievementRate,
                scoldedCount
        );
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
        LocalDate today = LocalDate.now();
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