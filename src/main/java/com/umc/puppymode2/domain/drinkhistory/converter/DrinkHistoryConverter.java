package com.umc.puppymode2.domain.drinkhistory.converter;

import com.umc.puppymode2.domain.drinkhistory.dto.DrinkHistoryRequestDTO;
import com.umc.puppymode2.domain.drinkhistory.dto.DrinkHistoryResponseDTO;
import com.umc.puppymode2.domain.drinkhistory.entity.DrinkHistory;

public class DrinkHistoryConverter {
    public static DrinkHistoryResponseDTO.DrinkStatus toStatusDTO(boolean yesterday, boolean today) {
        return DrinkHistoryResponseDTO.DrinkStatus.builder()
                .yesterdayRecorded(yesterday)
                .todayRecorded(today)
                .build();
    }

    public static DrinkHistory toEntity(Long userId, DrinkHistoryRequestDTO.CreateDrinkHistory dto) {
        return DrinkHistory.builder()
                .userId(userId)
                .isDrink(dto.getIsDrink())
                .drinkDate(dto.getDrinkDate())
                .build();
    }
}
