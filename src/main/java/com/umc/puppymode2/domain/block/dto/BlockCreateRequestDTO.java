package com.umc.puppymode2.domain.block.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BlockCreateRequestDTO {

    @NotNull(message = "차단할 사용자를 확인해주세요")
    private Long userId;
}
