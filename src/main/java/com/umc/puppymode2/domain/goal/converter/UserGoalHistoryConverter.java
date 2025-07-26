package com.umc.puppymode2.domain.goal.converter;

import com.umc.puppymode2.domain.goal.dto.GoalInfoResponseDTO;
import com.umc.puppymode2.domain.goal.dto.GoalPostRequestDTO;
import com.umc.puppymode2.domain.goal.entity.UserGoalHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class UserGoalHistoryConverter {

    public UserGoalHistory toEntity(GoalPostRequestDTO dto, Long userId, Long monthlyGoalCount) {

        if (dto.getGoal() == null) {
            throw new IllegalArgumentException("목표 값은 null일 수 없습니다.");
        }

        return UserGoalHistory.builder()
                .userId(userId)
                .monthlyGoalCount(dto.getGoal())
                .monthlyActualCount(monthlyGoalCount)
                .isGoalExceeded(false)
                .goalSetAt(LocalDateTime.now())
                .build();
    }


    public GoalInfoResponseDTO toDto(UserGoalHistory entity) {
        return GoalInfoResponseDTO.builder()
                .monthlyGoalCount(entity.getMonthlyGoalCount())
                .monthlyActualCount(entity.getMonthlyActualCount())
                .isGoalExceeded(entity.getIsGoalExceeded())
                .goalSetAt(entity.getGoalSetAt())
                .build();
    }
}