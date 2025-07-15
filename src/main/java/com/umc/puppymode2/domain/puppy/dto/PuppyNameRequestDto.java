package com.umc.puppymode2.domain.puppy.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class PuppyNameRequestDto {
    @NotBlank(message = "강아지 이름은 비어 있을 수 없습니다.")
    private String puppyName;
}
