package com.umc.puppymode2.domain.social.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SocialSummaryResponseDTO {
    private boolean hasBadge;         // pendingRequestCount > 0 또는 unreadCheerCount > 0 (홈 소셜 버튼의 빨간 원)
    private long pendingRequestCount; // 내가 받은 PENDING 친구 요청 수
    private long unreadCheerCount;    // 안 읽은 만료 전 응원 수
}
