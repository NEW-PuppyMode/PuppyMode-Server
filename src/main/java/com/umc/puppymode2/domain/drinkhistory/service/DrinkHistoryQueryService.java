package com.umc.puppymode2.domain.drinkhistory.service;

import com.umc.puppymode2.domain.drinkhistory.dto.DrinkHistoryResponseDTO;

public interface DrinkHistoryQueryService {
    DrinkHistoryResponseDTO.DrinkStatus getDrinkRecordStatus(Long userId);
}
