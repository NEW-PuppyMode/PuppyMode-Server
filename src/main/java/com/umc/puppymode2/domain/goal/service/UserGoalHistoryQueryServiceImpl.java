package com.umc.puppymode2.domain.goal.service;

import com.umc.puppymode2.domain.drinkhistory.repository.DrinkHistoryRepository;
import com.umc.puppymode2.domain.goal.converter.UserGoalHistoryConverter;
import com.umc.puppymode2.domain.goal.dto.GoalInfoResponseDTO;
import com.umc.puppymode2.domain.goal.entity.UserGoalHistory;
import com.umc.puppymode2.domain.goal.repository.UserGoalHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class UserGoalHistoryQueryServiceImpl implements UserGoalHistoryQueryService {
    private final UserGoalHistoryRepository repository;
    private final UserGoalHistoryConverter converter;
    private final DrinkHistoryRepository drinkHistoryRepository;


    @Override
    public GoalInfoResponseDTO getLatestGoal(Long userId) {
        UserGoalHistory latest = repository.findTopByUserIdOrderByGoalSetAtDesc(userId)
                .orElse(null);

        if (latest == null) return null;

        LocalDate firstDay = LocalDate.now().withDayOfMonth(1);
        LocalDate lastDay = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
        long actualCount = drinkHistoryRepository.countByUserUserIdAndDrinkDateBetween(userId, firstDay, lastDay);

        latest.setMonthlyActualCount(actualCount);
        return converter.toDto(latest);
    }

    @Override
    public boolean isMoreThan30DayPassed(Long userId) {
        return repository.findTopByUserIdOrderByGoalSetAtDesc(userId)
                .map(goal -> ChronoUnit.DAYS.between(goal.getGoalSetAt(), LocalDateTime.now()) >= 30)
                .orElse(true);

    }
}
