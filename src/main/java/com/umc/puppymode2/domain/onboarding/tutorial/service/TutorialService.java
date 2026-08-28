package com.umc.puppymode2.domain.onboarding.tutorial.service;

public interface TutorialService {

    /**
     * 튜토리얼 진행 상태를 "봤음"으로 등록
     *
     * 튜토리얼 화면 진입 시점에 호출하는 것을 전제로 함.
     * 끝까지 다 보고 나가든, 중간에 이탈하든 결과(다음부터 튜토리얼 노출x)는
     * 동일하므로 '완료'와 '이탈'을 구분해서 관리하지 않음.
     * 이미 true인 상태에서 재호출해도 안전함
     *
     * @param userId 사용자 ID
     */
    void markTutorialShown(Long userId);
}
