package com.umc.puppymode2.domain.block.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlockCreateResponseDTO {
    private Long userId;                // 차단한 사용자 ID
    private OffsetDateTime blockedAt;   // 차단 시각 (+09:00)
}
