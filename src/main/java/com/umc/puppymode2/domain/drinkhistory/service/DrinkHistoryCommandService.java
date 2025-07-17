package com.umc.puppymode2.domain.drinkhistory.service;

import com.umc.puppymode2.domain.drinkhistory.dto.DrinkHistoryRequestDTO;
import jakarta.validation.Valid;

public interface DrinkHistoryCommandService {
    Long recordDrink(Long userId, DrinkHistoryRequestDTO.CreateDrinkHistory dto);
}
