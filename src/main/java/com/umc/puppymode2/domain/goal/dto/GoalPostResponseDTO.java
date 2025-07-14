package com.umc.puppymode2.domain.goal.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoalPostResponseDTO {
    private boolean isSuccess;
    private String code;
    private String message;
}
