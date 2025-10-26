package com.umc.puppymode2.domain.drinkhistory.service;

import com.umc.puppymode2.domain.drinkhistory.dto.DrinkHistoryRequestDTO;
import com.umc.puppymode2.domain.drinkhistory.dto.DrinkHistoryResponseDTO;

public interface DrinkHistoryCommandService {
    DrinkHistoryResponseDTO recordDrink(Long userId, DrinkHistoryRequestDTO dto);
}
