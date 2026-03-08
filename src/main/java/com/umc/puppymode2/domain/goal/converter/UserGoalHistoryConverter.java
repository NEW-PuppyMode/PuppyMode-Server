package com.umc.puppymode2.domain.goal.converter;

import com.umc.puppymode2.domain.goal.dto.GoalInfoResponseDTO;
import com.umc.puppymode2.domain.goal.dto.GoalPostRequestDTO;
import com.umc.puppymode2.domain.goal.entity.UserGoalHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class UserGoalHistoryConverter {

    public UserGoalHistory toEntity(GoalPostRequestDTO dto, Long userId) {

        int goal = dto.getGoal();
        LocalDate now = LocalDate.now();
        LocalDate goalMonth = now.withDayOfMonth(1);

        return UserGoalHistory.builder()
                .userId(userId)
                .goalMonth(goalMonth)
                .monthlyGoalCount(goal)
                .goalSetAt(LocalDateTime.now())
                .build();
    }

    public GoalInfoResponseDTO toDto(UserGoalHistory entity, Long monthlyActualCount) {

        boolean exceeded = monthlyActualCount >= entity.getMonthlyGoalCount();

        return GoalInfoResponseDTO.builder()
                .monthlyGoalCount(entity.getMonthlyGoalCount())
                .monthlyActualCount(monthlyActualCount)
                .isGoalExceeded(exceeded)
                .goalSetAt(entity.getGoalSetAt())
                .build();
    }
}