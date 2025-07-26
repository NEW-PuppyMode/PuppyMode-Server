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

@Service
@RequiredArgsConstructor
public class DrinkReportServiceImpl implements DrinkReportService {
    private final UserGoalHistoryRepository userGoalHistoryRepository;
    private final DrinkHistoryRepository drinkHistoryRepository;
    private final DrinkReportConverter drinkReportConverter;

    @Transactional
    @Override
    public DrinkReportResponseDTO drinkReport(Long userId) {
        LocalDate now = LocalDate.now();
        LocalDate firstDayOfMonth = now.withDayOfMonth(1);
        LocalDate lastDayOfMonth = now.withDayOfMonth(now.lengthOfMonth());

        int goal = userGoalHistoryRepository.findTopByUserIdOrderByGoalSetAtDesc(userId)
                .map(UserGoalHistory::getMonthlyGoalCount)
                .orElse(15); // 기본값 15

        // 임시 값. 음주기록 구현되면 하기.
        Long drinkRecordCount = drinkHistoryRepository.countByUserUserIdAndDrinkDateBetween(userId, firstDayOfMonth, lastDayOfMonth);

        Long drinkDays = drinkHistoryRepository.countDistinctDrinkDates(userId, firstDayOfMonth, lastDayOfMonth);

        int achievementRate = 0;
        // 음주 횟수가 이번 달 목표를 넘겼으면 확률 0
        if (drinkDays >= goal){
            achievementRate = 0;
        }else{
            achievementRate = (int) (((double)(goal - drinkDays) / goal) * 100);
        }

        int scoldedCount = 0;

        DrinkReportResponseDTO dto = drinkReportConverter.toDto(goal, drinkRecordCount, drinkDays, achievementRate, scoldedCount);

        return dto;
    }
}
