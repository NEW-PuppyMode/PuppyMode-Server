package com.umc.puppymode2.domain.report.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DrinkReportResponseDTO {
    private Integer goal;
    private Long drinkRecordCount;
    private Long drinkDays;
    private int achievementRate;
    private int scoldedCount;

    // 조회 대상 월의 목표 대비 달성 상태 (NO_GOAL / IN_PROGRESS / ACHIEVED / FAILED)
    private GoalStatus goalStatus;

    // 월간 목표 달성 보상 전/후 레벨. 보상 없으면(실패, 목표없음, 진행 중) null
    private GoalRewardDTO goalReward;
}
