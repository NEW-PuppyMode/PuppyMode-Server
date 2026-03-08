package com.umc.puppymode2.domain.goal.service;

import com.umc.puppymode2.domain.drinkhistory.repository.DrinkHistoryRepository;
import com.umc.puppymode2.domain.goal.converter.UserGoalHistoryConverter;
import com.umc.puppymode2.domain.goal.dto.GoalInfoResponseDTO;
import com.umc.puppymode2.domain.goal.entity.UserGoalHistory;
import com.umc.puppymode2.domain.goal.repository.UserGoalHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class UserGoalHistoryQueryServiceImpl implements UserGoalHistoryQueryService {

    private final UserGoalHistoryRepository repository;
    private final UserGoalHistoryConverter converter;
    private final DrinkHistoryRepository drinkHistoryRepository;

    @Override
    public GoalInfoResponseDTO getLatestGoal(Long userId) {

        LocalDate now = LocalDate.now();
        LocalDate goalMonth = now.withDayOfMonth(1); // 이번 달 기준

        // 이번 달 목표 조회
        UserGoalHistory goal = repository
                .findByUserIdAndGoalMonth(userId, goalMonth)
                .orElse(null);

        if (goal == null) return null;

        // 이번 달 음주 횟수 계산
        LocalDate firstDay = goalMonth;
        LocalDate lastDay = goalMonth.withDayOfMonth(goalMonth.lengthOfMonth());

        long actualCount =
                drinkHistoryRepository.countByUserUserIdAndIsDrinkTrueAndDrinkDateBetween(
                        userId, firstDay, lastDay
                );

        return converter.toDto(goal, actualCount);
    }

    @Override
    public boolean isMoreThan30DayPassed(Long userId) {

        LocalDate goalMonth = LocalDate.now().withDayOfMonth(1);

        // 이번 달 목표 존재 여부 확인
        return repository.findByUserIdAndGoalMonth(userId, goalMonth).isEmpty();
    }
}