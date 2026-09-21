package com.umc.puppymode2.domain.cheer.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 응원 문구 분류. 선언 순서가 곧 응원 문구 조회 API의 분류 노출 순서(장난 → 위로 → 응원)다.
 */
@Getter
@RequiredArgsConstructor
public enum CheerCategory {
    PRANK("장난"),
    COMFORT("위로"),
    CHEER("응원");

    // 화면 탭 이름
    private final String label;
}
