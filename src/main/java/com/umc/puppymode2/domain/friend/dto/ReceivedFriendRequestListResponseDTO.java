package com.umc.puppymode2.domain.friend.dto;

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
        private String profileImageUrl;  // 요청자 프로필 이미지
        private OffsetDateTime requestedAt; // 요청 시각 (+09:00)
    }
}
