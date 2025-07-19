package com.umc.puppymode2.domain.report.service;

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
    // private final DrinkHistoryRepository drinkHistoryRepository; 음주 기록이 나오면 추가
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
        int drinkRecordCount = 0;

        int drinkDays = 0;

        int achievementRate = 0;

        int scoldedCount = 0;

        DrinkReportResponseDTO dto = drinkReportConverter.toDto(goal, drinkRecordCount, drinkDays, achievementRate, scoldedCount);

        return dto;
    }

    // 리포트 관련 로직 계산 -> 음주 기록 후


}
