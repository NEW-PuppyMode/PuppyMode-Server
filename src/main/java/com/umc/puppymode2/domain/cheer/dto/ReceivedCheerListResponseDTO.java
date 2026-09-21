package com.umc.puppymode2.domain.cheer.dto;

import com.umc.puppymode2.domain.cheer.entity.enums.CheerCategory;
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
public class ReceivedCheerListResponseDTO {

    private int count; // 받은 응원 수 (「받은 응원 N」 탭 배지)
    private List<Item> cheers;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Item {
        private Long cheerId;
        private Sender sender;
        private CheerCategory category;
        private String message;            // 발송 시점 문구 스냅샷
        private OffsetDateTime receivedAt; // 받은 시각 (+09:00)
        private OffsetDateTime expiresAt;  // 사라지는 시각 (+09:00)
        private CheerExpiresIn expiresIn;  // TODAY / TOMORROW
        // Lombok이 boolean isRead 필드의 getter를 isRead()로 만들면 JSON 키가 "read"가 되므로 Boolean 래퍼를 쓴다.
        private Boolean isRead;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Sender {
        private Long userId;
        private String username;
        private String profileImageUrl;
    }
}
