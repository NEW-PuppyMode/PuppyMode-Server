package com.umc.puppymode2.domain.puppy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Builder
@AllArgsConstructor
public class MainResponseDto {
    private int puppyLevel;          // 강아지 레벨
    private String puppyLevelName;   // 레벨 이름
    private int puppyLevelPercent;   // 경험치 진행률(%)
    private String puppyImageUrl;    // 강아지 레벨 이미지 URL
    private String currentPuppyName;

    @JsonProperty("isPuppyName")
    private boolean puppyName;     // 강아지 이름 지어주기 여부

    @JsonProperty("isMyName")
    private boolean myName;        // 내 이름 알려주기 여부

    @JsonProperty("isGoal")
    private boolean goal;          // 이번 달 목표 설정 여부

    private boolean didRecordYesterday; // 어제 기록 완료 여부
    private boolean didRecordToday;  // 오늘 기록 완료 여부
}
