package com.umc.puppymode2.domain.friend.repository;

import java.time.LocalDate;

// 친구 목록의 음주 상태 계산에 필요한 최소 컬럼만 가져오는 프로젝션 (음주 양/종류 등은 타인에게 노출하지 않는다)
public interface FriendDrinkRecordProjection {
    Long getUserId();
    LocalDate getDrinkDate();
    Boolean getIsDrink();
}
