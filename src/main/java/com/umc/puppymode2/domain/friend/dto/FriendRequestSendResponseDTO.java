package com.umc.puppymode2.domain.friend.dto;

import com.umc.puppymode2.domain.friend.entity.enums.FriendRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FriendRequestSendResponseDTO {
    private Long requestId;             // 친구 요청 ID (자동 수락 시에도 반환)
    private FriendRequestStatus status; // PENDING 또는 ACCEPTED
    private boolean autoAccepted;       // 상대가 이미 나에게 요청한 상태여서 즉시 친구가 됐는지
}
