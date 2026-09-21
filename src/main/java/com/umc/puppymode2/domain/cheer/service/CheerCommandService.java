package com.umc.puppymode2.domain.cheer.service;

import com.umc.puppymode2.domain.cheer.dto.CheerSendRequestDTO;
import com.umc.puppymode2.domain.cheer.dto.CheerSendResponseDTO;

public interface CheerCommandService {

    // 친구에게 응원을 보낸다.
    CheerSendResponseDTO sendCheer(Long myUserId, Long friendUserId, CheerSendRequestDTO request);

    // 내가 받은 안 읽은 응원을 모두 읽음 처리한다. (멱등)
    void markAllReceivedAsRead(Long myUserId);
}
