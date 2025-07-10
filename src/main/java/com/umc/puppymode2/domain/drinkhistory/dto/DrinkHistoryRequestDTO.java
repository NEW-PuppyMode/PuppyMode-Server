package com.umc.puppymode2.domain.drinkhistory.dto;

import com.umc.puppymode2.domain.temp.entity.enums.TempStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class DrinkHistoryRequestDTO {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateDrinkHistory {
        private LocalDate drinkDate;
        private Boolean isDrink;
    }
}
