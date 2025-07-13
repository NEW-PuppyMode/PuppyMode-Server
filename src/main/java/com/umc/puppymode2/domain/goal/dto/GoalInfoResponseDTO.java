package com.umc.puppymode2.domain.goal.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoalInfoResponseDTO {
    private Integer monthlyGoalCount;
    private Integer monthlyActualCount;
    private Boolean isGoalExceeded;
    private LocalDateTime goalSetAt;
}