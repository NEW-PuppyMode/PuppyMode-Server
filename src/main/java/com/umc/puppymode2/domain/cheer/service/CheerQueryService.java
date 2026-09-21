package com.umc.puppymode2.domain.cheer.service;

import com.umc.puppymode2.domain.cheer.dto.ReceivedCheerListResponseDTO;

public interface CheerQueryService {

    // 내가 받은 만료 전 응원 목록 (받은 순서 최신순)
    ReceivedCheerListResponseDTO getReceivedCheers(Long myUserId);
}
