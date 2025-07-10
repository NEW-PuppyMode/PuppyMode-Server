package com.umc.puppymode2.domain.drinkhistory.dto;

import lombok.*;

public class DrinkHistoryResponseDTO {
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DrinkStatus {
        private boolean yesterdayRecorded;
        private boolean todayRecorded;
    }

}
