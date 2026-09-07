package com.umc.puppymode2.domain.goal.converter;

import com.umc.puppymode2.domain.goal.dto.GoalInfoResponseDTO;
import com.umc.puppymode2.domain.goal.dto.GoalPostRequestDTO;
import com.umc.puppymode2.domain.goal.entity.UserGoalHistory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class UserGoalHistoryConverter {

    public UserGoalHistory toEntity(
            GoalPostRequestDTO dto,
            Long userId,
            LocalDate goalMonth,
            LocalDateTime goalSetAt
    ) {

        return UserGoalHistory.builder()
                .userId(userId)
                .goalMonth(goalMonth)
                .monthlyGoalCount(dto.getGoal())
                .goalSetAt(goalSetAt)
                .build();
    }

    /**
     * 최근 목표 조회(GET /goals) 응답 DTO를 만든다.
     *
     * @param currentMonthGoal   이번 달에 설정된 목표. 없으면 null → 앞쪽 4개 필드를 전부 null로 둔다.
     * @param monthlyActualCount 이번 달 실제 음주 횟수. currentMonthGoal이 null이면 계산하지 않으므로 null이 넘어온다.
     * @param latestGoal         가장 최근에 설정한 목표(월 기준 내림차순 1건). 목표 이력이 전혀 없으면 null
     *                           → latestGoalYear/latestGoalMonth를 null로 둔다.
     */
    public GoalInfoResponseDTO toDto(
            UserGoalHistory currentMonthGoal,
            Long monthlyActualCount,
            UserGoalHistory latestGoal
    ) {

        GoalInfoResponseDTO.GoalInfoResponseDTOBuilder builder = GoalInfoResponseDTO.builder();

        // 이번 달 목표가 있을 때만 목표/실적/초과 여부/설정 시각을 채운다.
        if (currentMonthGoal != null) {
            boolean exceeded = monthlyActualCount != null
                    && monthlyActualCount >= currentMonthGoal.getMonthlyGoalCount();

            builder.monthlyGoalCount(currentMonthGoal.getMonthlyGoalCount())
                    .monthlyActualCount(monthlyActualCount)
                    .isGoalExceeded(exceeded)
                    .goalSetAt(currentMonthGoal.getGoalSetAt());
        }

        // 최근 목표가 하나라도 있으면 그 연/월을 채운다. (goalMonth는 항상 해당 월 1일로 저장됨)
        if (latestGoal != null) {
            LocalDate latestGoalMonth = latestGoal.getGoalMonth();
            builder.latestGoalYear(latestGoalMonth.getYear())
                    .latestGoalMonth(latestGoalMonth.getMonthValue());
        }

        return builder.build();
    }
}