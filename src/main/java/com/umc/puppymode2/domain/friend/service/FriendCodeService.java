package com.umc.puppymode2.domain.friend.service;

import com.umc.puppymode2.domain.friend.dto.FriendCodeResponseDTO;

public interface FriendCodeService {

    // 내 친구 코드를 조회한다. 아직 없으면 이 시점에 발급한다.
    FriendCodeResponseDTO getOrIssueMyCode(Long userId);
}
