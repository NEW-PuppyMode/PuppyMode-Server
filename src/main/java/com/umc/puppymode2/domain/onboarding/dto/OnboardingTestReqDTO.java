package com.umc.puppymode2.domain.onboarding.dto;

import java.util.List;

public record OnboardingTestReqDTO(

        List<OnboardingTestAnswerDTO> answers
) {
}
