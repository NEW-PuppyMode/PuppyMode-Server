package com.umc.puppymode2.domain.friend.dto;

// 친구 목록의 「응원하기」 버튼 상태
public enum FriendCheerState {
    ACTIVE,   // 응원 가능 (어제/오늘 음주했고 그 날짜에 아직 응원을 안 보냄)
    SENT,     // 어제/오늘 음주했지만 해당 날짜 응원을 모두 보냄
    DISABLED  // 어제/오늘 음주 기록 없음
}
