package com.umc.puppymode2.domain.goal.service;

import com.umc.puppymode2.domain.goal.dto.GoalInfoResponseDTO;

import java.util.List;

public interface UserGoalHistoryQueryService {
    GoalInfoResponseDTO getLatestGoal(Long userId);
    boolean isMoreThan30DayPassed(Long userId);

    // 목표가 설정된 월 목록을 "yyyy-MM" 문자열 오름차순으로 반환한다. 목표 이력이 없으면 빈 리스트.
    List<String> getGoalMonths(Long userId);
}
