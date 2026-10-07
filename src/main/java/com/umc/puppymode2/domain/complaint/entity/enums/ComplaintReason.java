package com.umc.puppymode2.domain.complaint.entity.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

// TODO: 신고 사유 값(영문 이름)은 임시 값. 클라이언트와 확정 필요 (#193)
@Getter
@AllArgsConstructor
public enum ComplaintReason {
    INAPPROPRIATE_NICKNAME("부적절한 닉네임"),
    INAPPROPRIATE_PUPPY_NAME("부적절한 강아지 이름"),
    ABUSE_HARASSMENT("욕설·괴롭힘"),
    SPAM_AD("스팸·광고"),
    OTHER("기타");

    private final String label;
}
