package com.umc.puppymode2.domain.user.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "현재 사용자 상태 조회 응답 DTO")
public record AuthMeResponseDTO(

        @Schema(
                description = "온보딩 완료 여부 (deprecated, isBreedTestDone 사용 예정)",
                example = "true"
        )
        boolean isOnboarded,

        @Schema(
                description = "강아지 유형 테스트 완료 여부",
                example = "true"
        )
        boolean isBreedTestDone
) {
}