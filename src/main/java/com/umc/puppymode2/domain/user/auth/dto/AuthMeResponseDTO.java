package com.umc.puppymode2.domain.user.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "현재 사용자 상태 조회 응답 DTO")
public record AuthMeResponseDTO(

        @Schema(
                description = "온보딩 완료 여부 (deprecated + 신규 개발 시에는 isPuppyTestCompledted)",
                example = "true"
        )
        boolean isOnboarded,

        @Schema(
                description = "강아지 유형 검사 완료 여부 (isOnboarded와 동일한 값)",
                example = "true"
        )
        boolean isPuppyTestCompleted,

        @Schema(
                description = "온보딩(강아지 이름/내 이름/목표 설정) 완료 여부",
                example = "false"
        )
        boolean onboardingCompleted,

        @Schema(
                description = "튜토리얼 진행 여부",
                example = "false"
        )
        boolean tutorialShown
) {
}