package com.umc.puppymode2.domain.goal.dto;

import com.umc.puppymode2.domain.report.dto.GoalRewardDTO;
import lombok.*;

/**
 * POST /goals/reward 응답
 *
 * rewarded = true : 이번 요청으로 지급했거나, 이미 지급받은 달 (goalReward에 before/after)
 * rewarded = false : 보상 조건 미달 / 목표 없음 (goalReward = null)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlyGoalRewardResponseDTO {
    private boolean rewarded;
    private GoalRewardDTO goalReward;
}
