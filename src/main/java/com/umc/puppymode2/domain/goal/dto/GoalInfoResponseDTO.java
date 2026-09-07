package com.umc.puppymode2.domain.goal.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoalInfoResponseDTO {
    private Integer monthlyGoalCount;
    private Long monthlyActualCount;
    private Boolean isGoalExceeded;
    private LocalDateTime goalSetAt;

    // 사용자가 가장 최근에 설정한 목표의 연도/월.
    // 월간 리포트가 GET /report?year&month 를 호출할 때 인자로 사용한다.
    // "지난달"로 단순 계산하지 않는 이유: 8월에 목표를 세우고 앱에 안 들어오다가
    // 11월에 접속하는 등의 케이스에서도 마지막으로 목표를 세운 달을 가리켜야 하기 때문.
    // 목표 이력이 전혀 없는 신규 유저는 둘 다 null.
    private Integer latestGoalYear;
    private Integer latestGoalMonth;
}