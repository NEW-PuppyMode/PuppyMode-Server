package com.umc.puppymode2.domain.report.dto;

/**
 * 특정 월의 "목표 대비 달성 상태".
 *
 * 월간 리포트(GET /report)와 새 음주 캘린더 화면에서 목표 달성 여부를 표시하는 데 사용한다.
 * JSON 응답에는 enum name 문자열("IN_PROGRESS" 등)로 직렬화된다.
 */
public enum GoalStatus {

    /** 조회 대상 월에 설정된 목표가 없음 */
    NO_GOAL,

    /** 조회 대상 월이 이번 달(KST 기준)이며 목표가 있음 — 아직 진행 중이라 성패를 확정할 수 없음 */
    IN_PROGRESS,

    /** 과거 월이고, 실제 음주일 수가 목표 이하 — 달성 */
    ACHIEVED,

    /** 과거 월이고, 실제 음주일 수가 목표 초과 — 미달성 (음주 기록이 아예 없는 경우도 여기 포함) */
    FAILED
}
