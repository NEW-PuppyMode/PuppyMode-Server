package com.umc.puppymode2.domain.friend.event;

public record FriendRequestReceivedEvent(Long receiverId, Long requesterId) {
}
