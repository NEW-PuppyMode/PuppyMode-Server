package com.umc.puppymode2.domain.report.converter;

import com.umc.puppymode2.domain.report.dto.DrinkReportResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class DrinkReportConverter {
    public DrinkReportResponseDTO toDto(Integer goal, Long drinkCount, Long drinkDays, Integer successProbability, Integer scoldedCount) {
        return DrinkReportResponseDTO.builder()
                .goal(goal)
                .drinkRecordCount(drinkCount)
                .drinkDays(drinkDays)
                .successProbability(successProbability)
                .scoldedCount(scoldedCount)
                .build();
    }
}
