package com.umc.puppymode2.domain.onboarding.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record OnboardingTestAnswerDTO(

        @Min(value = 1, message = "질문 번호는 1 이상이어야 합니다.")
        @Max(value = 6, message = "질문 번호는 6 이하여야 합니다.")
        int questionId, // 질문 번호(순서)

        @Min(value = 1, message = "답변은 1 또는 2만 가능합니다.")
        @Max(value = 2, message = "답변은 1 또는 2만 가능합니다.")
        int answer // 사용자의 선택지 (1 or 2)
) {
}
