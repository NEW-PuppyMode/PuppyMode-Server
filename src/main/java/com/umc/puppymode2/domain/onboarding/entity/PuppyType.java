package com.umc.puppymode2.domain.onboarding.entity;

import lombok.Getter;

// 관련된 엔티티/enum/repository 가 미리 생성되지 않아 임시로 구현한 파일입니다.
// 이후 다른 분들 코드도 머지되면, 해당 코드 속 파일로 import 변경하도록 하겠습니다!
@Getter
public enum PuppyType {

    BICHON("분위기 리더형", "비숑", "술자리를 이끄는 활발하고 감성적인 리더!", "https://example.image.url.com"),
    SHIBA("은밀한 취객형", "시바견", "혼술을 사랑하는 섬세한 감성가!", "https://example.image.url.com"),
    CORGI("컨트롤러형", "웰시코기", "술을 도구처럼 다루는 전략가!", "https://example.image.url.com"),
    POODLE("관찰자형", "푸들", "술자리에서도 분석과 기록을 놓치지 않는 관찰자!", "https://example.image.url.com");

    private final String type; // 성향 이름
    private final String breed; // 견종
    private final String description; // 소개 문구
    private final String imageUrl; // 강아지 대표 이미지

    PuppyType(String type, String breed, String description, String imageUrl) {
        this.type = type;
        this.breed = breed;
        this.description = description;
        this.imageUrl = imageUrl;
    }
}
