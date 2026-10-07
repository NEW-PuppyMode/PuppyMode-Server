package com.umc.puppymode2.domain.block.service;

import com.umc.puppymode2.domain.block.dto.BlockListResponseDTO;

public interface BlockQueryService {

    // 내가 차단한 사용자 목록 (최근 차단순)
    BlockListResponseDTO getBlocks(Long myUserId);
}
