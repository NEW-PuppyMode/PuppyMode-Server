package com.umc.puppymode2.domain.friend.dto;

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
public class ReceivedFriendRequestListResponseDTO {

    private int count; // 받은 요청 수 (「받은 요청 N」)
    private List<Item> requests;

    @Schema(name = "ReceivedFriendRequestItem")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Item {
        private Long requestId;
        private Long userId;             // 요청자 ID
        private String username;         // 요청자 이름
        private String puppyName;        // 요청자 강아지 이름
        private Integer level;           // 요청자 강아지 레벨
        private PuppyType puppyType;     // 요청자 강아지 종류 (강아지가 없으면 null)
        private OffsetDateTime requestedAt; // 요청 시각 (+09:00)
    }
}
