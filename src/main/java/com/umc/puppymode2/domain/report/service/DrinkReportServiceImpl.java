package com.umc.puppymode2.domain.report.service;

import com.umc.puppymode2.domain.drinkhistory.repository.DrinkHistoryRepository;
import com.umc.puppymode2.domain.goal.entity.UserGoalHistory;
import com.umc.puppymode2.domain.goal.repository.UserGoalHistoryRepository;
import com.umc.puppymode2.domain.advice.repository.AdviceRepository;
import com.umc.puppymode2.domain.report.converter.DrinkReportConverter;
import com.umc.puppymode2.domain.report.dto.DrinkReportResponseDTO;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;

@Service
@RequiredArgsConstructor
public class DrinkReportServiceImpl implements DrinkReportService {

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

        int goal = userGoalHistoryRepository
                .findTopByUserIdAndGoalSetAtLessThanEqualOrderByGoalSetAtDesc(userId, endDateTime)
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

        int achievementRate = calculateAchievementRate(goal, drinkDays);

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
     * 절주 목표 달성률 계산
     * 목표 대비 실제 음주일이 얼마나 적은지 계산
     */
    private int calculateAchievementRate(int goal, long drinkDays) {

        if (goal <= 0) {
            return 0;
        }

        double rate = ((double) (goal - drinkDays) / goal) * 100;

        return (int) Math.max(rate, 0);
    }
}