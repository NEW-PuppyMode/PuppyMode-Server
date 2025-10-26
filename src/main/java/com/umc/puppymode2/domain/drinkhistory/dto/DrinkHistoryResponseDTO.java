package com.umc.puppymode2.domain.drinkhistory.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DrinkHistoryResponseDTO {
    private Long drinkHistoryId;
    private Integer puppyExp;
}
