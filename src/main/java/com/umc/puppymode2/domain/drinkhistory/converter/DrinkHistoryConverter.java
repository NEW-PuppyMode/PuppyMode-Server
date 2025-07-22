package com.umc.puppymode2.domain.drinkhistory.converter;

import com.umc.puppymode2.domain.drinkhistory.dto.DrinkHistoryRequestDTO;
import com.umc.puppymode2.domain.drinkhistory.dto.DrinkHistoryStatusDTO;
import com.umc.puppymode2.domain.drinkhistory.entity.DrinkHistory;
import com.umc.puppymode2.domain.user.entity.User;

public class DrinkHistoryConverter {
    public static DrinkHistoryStatusDTO toStatusDTO(boolean yesterday, boolean today) {
        return DrinkHistoryStatusDTO.builder()
                .yesterdayRecorded(yesterday)
                .todayRecorded(today)
                .build();
    }

    public static DrinkHistory toEntity(User user, DrinkHistoryRequestDTO dto) {
        return DrinkHistory.builder()
                .user(user)
                .isDrink(dto.getIsDrink())
                .drinkDate(dto.getDrinkDate())
                .build();
    }
}
