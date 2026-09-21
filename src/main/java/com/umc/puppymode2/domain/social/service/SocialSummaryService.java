package com.umc.puppymode2.domain.social.service;

import com.umc.puppymode2.domain.social.dto.SocialSummaryResponseDTO;

public interface SocialSummaryService {

    // 홈 소셜 버튼의 뱃지(빨간 원) 표시 여부와 건수를 조회한다.
    SocialSummaryResponseDTO getSummary(Long myUserId);
}
