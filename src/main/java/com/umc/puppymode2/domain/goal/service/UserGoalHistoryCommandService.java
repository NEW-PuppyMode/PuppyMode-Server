package com.umc.puppymode2.domain.goal.service;

import com.umc.puppymode2.domain.goal.dto.GoalPostRequestDTO;
import com.umc.puppymode2.domain.goal.dto.GoalPostResponseDTO;

public interface UserGoalHistoryCommandService {
    GoalPostResponseDTO postGoal(Long userId, GoalPostRequestDTO goalPostRequestDTO);
}
