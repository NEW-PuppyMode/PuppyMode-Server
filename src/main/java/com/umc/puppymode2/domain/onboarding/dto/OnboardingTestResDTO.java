package com.umc.puppymode2.domain.onboarding.dto;

public record OnboardingTestResDTO(

        String type, // 예: "분위기 리더형"
        String puppyBreed, // 예: "비숑 프리제"
        String description, // 타입에 대한 설명
        String imageUrl // 강아지 이미지 url
) {
}
