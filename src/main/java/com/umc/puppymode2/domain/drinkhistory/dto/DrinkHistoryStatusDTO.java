package com.umc.puppymode2.domain.drinkhistory.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DrinkHistoryStatusDTO {
    private boolean yesterdayRecorded;
    private boolean todayRecorded;

}
