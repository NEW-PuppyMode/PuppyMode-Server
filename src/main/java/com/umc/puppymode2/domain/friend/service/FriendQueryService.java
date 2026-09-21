package com.umc.puppymode2.domain.friend.service;

import com.umc.puppymode2.domain.friend.dto.FriendListResponseDTO;
import com.umc.puppymode2.domain.friend.dto.FriendProfileResponseDTO;
import com.umc.puppymode2.domain.friend.dto.ReceivedFriendRequestListResponseDTO;

public interface FriendQueryService {

    // 내가 받은 PENDING 친구 요청 목록 (최신순)
    ReceivedFriendRequestListResponseDTO getReceivedRequests(Long myUserId);

    // 내 친구 목록 (가나다순) + 음주 문구/응원 버튼 상태
    FriendListResponseDTO getFriends(Long myUserId);

    // 친구 프로필 (친구만 조회 가능)
    FriendProfileResponseDTO getFriendProfile(Long myUserId, Long friendUserId);
}
