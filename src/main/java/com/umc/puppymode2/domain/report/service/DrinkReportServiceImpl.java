package com.umc.puppymode2.domain.report.service;

import com.umc.puppymode2.domain.drinkhistory.repository.DrinkHistoryRepository;
import com.umc.puppymode2.domain.goal.entity.UserGoalHistory;
import com.umc.puppymode2.domain.goal.repository.UserGoalHistoryRepository;
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
    private final DrinkReportConverter drinkReportConverter;

    @Transactional(Transactional.TxType.SUPPORTS)
    @Override
    public DrinkReportResponseDTO drinkReport(Long userId, YearMonth targetMonth) {

        LocalDate firstDayOfMonth = targetMonth.atDay(1);
        LocalDate lastDayOfMonth = targetMonth.atEndOfMonth();

        // asOf 기준 시간을 LocalDateTime으로 변환 (그 달의 마지막 순간)
        LocalDateTime asOfDateTime = lastDayOfMonth.atTime(LocalTime.MAX);

        int goal = userGoalHistoryRepository
                .findTopByUserIdAndGoalSetAtLessThanEqualOrderByGoalSetAtDesc(userId, asOfDateTime)
                .map(UserGoalHistory::getMonthlyGoalCount)
                .orElse(15); // 기본값 15

        long drinkRecordCount = drinkHistoryRepository.countByUserUserIdAndDrinkDateBetween(userId, firstDayOfMonth, lastDayOfMonth);

        long drinkDays = drinkHistoryRepository.countDistinctDrinkDates(userId, firstDayOfMonth, lastDayOfMonth);

        // 절주 목표 유지율! (마신 비율이 아니라, 목표대비 안마신 비율!) 최대한 안마시는게 목표
        int achievementRate;
        if (goal <= 0 || drinkDays >= goal) {
            achievementRate = 0;
        } else {
            achievementRate = (int) (((double) (goal - drinkDays) / goal) * 100);
        }

        // TODO : 패널티 횟수 -> 한마디 횟수
        int scoldedCount = 0;

        DrinkReportResponseDTO dto = drinkReportConverter.toDto(goal, Long.valueOf(drinkRecordCount), Long.valueOf(drinkDays), achievementRate, scoldedCount);

        return dto;
    }
}
