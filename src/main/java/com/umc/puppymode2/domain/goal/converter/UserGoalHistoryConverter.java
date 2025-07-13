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

    public UserGoalHistory toEntity(GoalPostRequestDTO dto, Long userId) {
        return UserGoalHistory.builder()
                .userId(userId)
                .monthlyGoalCount(dto.getGoal())         // DTO에서 받은 목표
                .monthlyActualCount(0)                   // 기본값 0
                .isGoalExceeded(false)                   // 초기에는 목표 초과 X
                .goalSetAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
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