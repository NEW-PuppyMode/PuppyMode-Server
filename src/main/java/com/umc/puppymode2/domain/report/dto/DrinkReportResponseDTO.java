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
}
