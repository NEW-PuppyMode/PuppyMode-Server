package com.umc.puppymode2.domain.friend.service;

import com.umc.puppymode2.domain.friend.dto.FriendRequestAcceptResponseDTO;
import com.umc.puppymode2.domain.friend.dto.FriendRequestSendResponseDTO;

public interface FriendCommandService {

    // 친구 코드로 친구 요청을 보낸다. 상대가 이미 나에게 요청했다면 자동 수락한다.
    FriendRequestSendResponseDTO sendFriendRequest(Long myUserId, String friendCode);

    // 받은 친구 요청을 수락한다. (요청 수락 + 친구 관계 생성을 한 트랜잭션으로 처리)
    FriendRequestAcceptResponseDTO acceptFriendRequest(Long myUserId, Long requestId);

    // 받은 친구 요청을 거절한다. 요청자에게는 알리지 않는다.
    void rejectFriendRequest(Long myUserId, Long requestId);

    // 친구를 삭제한다. 차단이 아니므로 이후 다시 친구 요청을 할 수 있다.
    void deleteFriend(Long myUserId, Long friendUserId);
}
