package com.umc.puppymode2.domain.onboarding.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record OnboardingTestReqDTO(

        @NotEmpty(message = "답변 리스트는 비어있을 수 없습니다.")
        @Size(min = 6, max = 6, message = "정확히 6개의 답변이 필요합니다.")
        @Valid
        List<OnboardingTestAnswerDTO> answers
) {
}
