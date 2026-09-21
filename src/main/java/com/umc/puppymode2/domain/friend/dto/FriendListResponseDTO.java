package com.umc.puppymode2.domain.friend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FriendListResponseDTO {

    private int count; // 친구 수 (「내 친구 N」)
    private List<Item> friends;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Item {
        private Long userId;
        private String username;
        private String puppyName;
        private Integer level;
        private String profileImageUrl;
        private FriendDrinkStatus drinkStatus;
        private Cheer cheer;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Cheer {
        private FriendCheerState state;
        // ACTIVE일 때만 응원 대상 날짜(어제가 남아 있으면 어제 우선), 그 외에는 null
        private LocalDate targetDate;
    }
}
