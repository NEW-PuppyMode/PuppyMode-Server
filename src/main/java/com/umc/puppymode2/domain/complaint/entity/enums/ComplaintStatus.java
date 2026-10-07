package com.umc.puppymode2.domain.complaint.entity.enums;

// 어드민이 없어 운영자가 DB에서 직접 변경한다.
public enum ComplaintStatus {
    RECEIVED,
    IN_REVIEW,
    RESOLVED,
    DISMISSED
}
