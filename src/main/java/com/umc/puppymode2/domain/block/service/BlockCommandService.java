package com.umc.puppymode2.domain.block.service;

import com.umc.puppymode2.domain.block.dto.BlockCreateResponseDTO;

public interface BlockCommandService {

    // 친구를 차단한다. 친구 관계·둘 사이 응원·대기 중 친구 요청을 함께 삭제한다.
    BlockCreateResponseDTO block(Long myUserId, Long targetUserId);

    // 차단을 해제한다. 친구 관계는 복구하지 않는다.
    void unblock(Long myUserId, Long targetUserId);
}
