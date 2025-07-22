package com.umc.puppymode2.domain.drinkhistory.service;

import com.umc.puppymode2.domain.drinkhistory.dto.DrinkHistoryRequestDTO;

public interface DrinkHistoryCommandService {
    Long recordDrink(Long userId, DrinkHistoryRequestDTO dto);
}
