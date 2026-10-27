package com.umc.puppymode2.domain.report.dto;

import lombok.*;

/**
 * 월간 목표 달성 보상 지급 전/후의 강아지 레벨 상태
 *
 * MonthlyGoalScheduler에서 보상을 지급할 때, UserGoalHistory에 저장한 경험치 스냅샷으로 계산.
 * 보상 없는 달(실패, 목표 없음, 진행 중)엔 리포트 응답에서 null.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoalRewardDTO {
    private LevelSnapshotDTO before;
    private LevelSnapshotDTO after;
}
