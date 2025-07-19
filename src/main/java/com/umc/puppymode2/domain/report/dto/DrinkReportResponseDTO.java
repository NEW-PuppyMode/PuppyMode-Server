package com.umc.puppymode2.domain.report.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DrinkReportResponseDTO {
    private Integer goal;
    private Integer drinkRecordCount;
    private Integer drinkDays;
    private Integer achievementRate;
    private Integer scoldedCount;
}
