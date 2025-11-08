package com.umc.puppymode2.domain.onboarding.dto;

public record OnboardingTestResDTO(

        String type, // 예: "분위기 리더형"
        String puppyBreedKo, // 예: "비숑 프리제"
        String puppyBreedEn, // 예: "Bichon Frisé"
        String description, // 타입 설명, 예: "술자리를 이끄는 사람들"
        String imageUrl // 강아지 이미지 url
) {
}
