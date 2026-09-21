package com.umc.puppymode2.domain.friend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FriendRequestAcceptResponseDTO {
    private Long friendUserId; // 새 친구의 userId
}
