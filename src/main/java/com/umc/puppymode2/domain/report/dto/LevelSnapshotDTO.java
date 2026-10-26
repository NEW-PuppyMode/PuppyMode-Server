package com.umc.puppymode2.domain.report.dto;

import lombok.*;

/**
 * 특정 시점의 강아지 레벨과 레벨 내 진행률
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LevelSnapshotDTO {
    private int level;
    private int percent;
}
