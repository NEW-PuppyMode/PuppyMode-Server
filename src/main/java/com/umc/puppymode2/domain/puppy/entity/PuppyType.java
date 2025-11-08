package com.umc.puppymode2.domain.puppy.entity;

import lombok.Getter;

@Getter
public enum PuppyType {

    BICHON("분위기 리더형", "비숑 프리제", "Bichon Frisé", "술자리를 이끄는 사람들", "https://puppy-mode-s3-bucket.s3.ap-northeast-2.amazonaws.com/bichon_default_level1.svg"),
    SHIBA("은밀한 취객형", "시바견", "Shiba Inu", "혼술 장인", "https://puppy-mode-s3-bucket.s3.ap-northeast-2.amazonaws.com/shiba_default_level1.svg"),
    CORGI("컨트롤러형", "웰시코기", "Welsh Corgi", "술은 도구, 취함은 선택", "https://puppy-mode-s3-bucket.s3.ap-northeast-2.amazonaws.com/corgi_default_level1.svg"),
    POODLE("관찰자형", "푸들", "Poodle", "분석하고 기록하는 술자리 학자들", "https://puppy-mode-s3-bucket.s3.ap-northeast-2.amazonaws.com/poodle_default_level1.svg");

    private final String type; // 성향 이름
    private final String breedKo; // 견종 (한글명)
    private final String breedEn; // 견종 (영문명)
    private final String description; // 성향 설명
    private final String imageUrl; // 강아지 대표 이미지

    PuppyType(String type, String breedKo, String breedEn, String description, String imageUrl) {
        this.type = type;
        this.breedKo = breedKo;
        this.breedEn = breedEn;
        this.description = description;
        this.imageUrl = imageUrl;
    }
}
