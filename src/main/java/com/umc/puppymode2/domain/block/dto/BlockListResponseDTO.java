package com.umc.puppymode2.domain.block.dto;

import com.umc.puppymode2.domain.puppy.entity.PuppyType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlockListResponseDTO {

    private int count; // 차단한 사용자 수
    private List<Item> blocks;

    @Schema(name = "BlockListItem")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Item {
        private Long userId;
        private String username;
        private String puppyName;        // 강아지가 없으면 null
        private Integer level;
        private PuppyType puppyType;     // 강아지가 없으면 null
        private OffsetDateTime blockedAt; // 차단 시각 (+09:00)
    }
}
