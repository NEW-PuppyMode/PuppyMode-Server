package com.umc.puppymode2.domain.calendar.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalendarResponseDTO {
    private LocalDate date;

    @JsonProperty("isDrink")
    private boolean isDrink;
}
