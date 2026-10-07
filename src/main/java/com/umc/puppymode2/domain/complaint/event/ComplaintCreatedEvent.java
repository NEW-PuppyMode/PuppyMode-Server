package com.umc.puppymode2.domain.complaint.event;

import com.umc.puppymode2.domain.complaint.entity.enums.ComplaintReason;

/**
 * 신고가 접수(커밋)된 뒤 Slack 알림을 보내기 위한 이벤트.
 * TODO: 상세 사유(detail) 텍스트를 Slack에 포함할지 미확정이라 일단 담지 않는다. (#193)
 */
public record ComplaintCreatedEvent(
        Long complaintId,
        Long reporterId,
        Long targetUserId,
        String targetUsername,
        String targetPuppyName,
        ComplaintReason reason
) {
}
