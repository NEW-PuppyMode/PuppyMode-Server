package com.umc.puppymode2.domain.friend.dto;

// 친구 목록의 음주 문구 표시용 상태. 우선순위: DRANK_TODAY > DRANK_YESTERDAY > NOT_DRANK > NONE
public enum FriendDrinkStatus {
    DRANK_TODAY,     // 오늘 is_drink = true
    DRANK_YESTERDAY, // 어제 is_drink = true
    NOT_DRANK,       // 오늘 기록이 있고 is_drink = false
    NONE             // 위에 해당 없음 (오늘 기록 없음)
}
