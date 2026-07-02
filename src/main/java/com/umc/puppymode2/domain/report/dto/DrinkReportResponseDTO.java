package com.umc.puppymode2.domain.report.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DrinkReportResponseDTO {
    private Integer goal;
    private Long drinkRecordCount;
    private Long drinkDays;
    private int achievementRate;
    private int scoldedCount;
}
