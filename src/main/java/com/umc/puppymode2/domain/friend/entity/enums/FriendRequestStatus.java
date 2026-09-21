package com.umc.puppymode2.domain.friend.entity.enums;

public enum FriendRequestStatus {
    PENDING,  // 응답 대기
    ACCEPTED, // 수락됨 (친구 관계가 생성된 상태)
    REJECTED  // 거절됨 (요청자에게는 알리지 않으며, 요청자가 다시 요청하면 PENDING으로 되돌린다)
}
