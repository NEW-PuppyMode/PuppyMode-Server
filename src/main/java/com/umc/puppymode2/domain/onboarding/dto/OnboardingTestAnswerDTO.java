package com.umc.puppymode2.domain.onboarding.dto;

public record OnboardingTestAnswerDTO(

        int questionId, // 질문 번호(순서)
        int answer // 사용자의 선택지 (1 or 2)
) {
}
