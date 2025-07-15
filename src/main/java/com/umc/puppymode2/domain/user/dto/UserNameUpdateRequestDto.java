package com.umc.puppymode2.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class UserNameUpdateRequestDto {

    @NotBlank(message = "유저 이름은 비어 있을 수 없습니다.")
    private String myName;
}
