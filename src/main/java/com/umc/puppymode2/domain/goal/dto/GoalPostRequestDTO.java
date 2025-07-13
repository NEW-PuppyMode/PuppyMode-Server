package com.umc.puppymode2.domain.goal.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

    @Min(value = 0, message = "목표는 0 이상이어야 합니다.")
    @Max(value = 30, message = "목표는 30 이하여야 합니다.")
    private Integer goal; // 새로운 목표 설정 시만 필요
}
