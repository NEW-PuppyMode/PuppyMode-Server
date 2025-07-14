package com.umc.puppymode2.domain.goal.service;

import com.umc.puppymode2.domain.goal.dto.GoalInfoResponseDTO;

public interface UserGoalHistoryQueryService {
    GoalInfoResponseDTO getLatestGoal(Long userId);
    boolean isMoreThan30DayPassed(Long userId);
}
