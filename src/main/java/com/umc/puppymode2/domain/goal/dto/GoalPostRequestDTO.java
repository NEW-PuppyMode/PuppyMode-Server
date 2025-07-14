package com.umc.puppymode2.domain.goal.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class GoalPostRequestDTO {
    @NotNull(message = "isNew 값은 필수입니다.")
    private Boolean isNew; // true: 새로운 목표, false: 기존 목표 유지
    private Integer goal; // 새로운 목표 설정 시만 필요
}
