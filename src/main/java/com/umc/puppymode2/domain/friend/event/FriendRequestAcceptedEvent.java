package com.umc.puppymode2.domain.friend.event;

public record FriendRequestAcceptedEvent(Long receiverId, Long accepterId) {
}
