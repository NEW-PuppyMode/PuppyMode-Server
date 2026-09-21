package com.umc.puppymode2.domain.cheer.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CheerSendRequestDTO {

    @NotNull(message = "templateId는 필수입니다.")
    private Long templateId; // 선택한 문구 ID

    // 응원 대상 음주 날짜(yyyy-MM-dd). 친구 목록의 cheer.targetDate 값을 그대로 전달한다.
    @NotNull(message = "targetDate는 필수입니다.")
    private LocalDate targetDate;
}
