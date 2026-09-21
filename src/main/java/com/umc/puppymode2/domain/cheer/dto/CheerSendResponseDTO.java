package com.umc.puppymode2.domain.cheer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheerSendResponseDTO {
    private Long cheerId;          // 생성된 응원 ID
    private OffsetDateTime sentAt; // 발송 시각 (+09:00)
}
