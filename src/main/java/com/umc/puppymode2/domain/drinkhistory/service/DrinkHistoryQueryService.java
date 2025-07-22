package com.umc.puppymode2.domain.drinkhistory.service;

import com.umc.puppymode2.domain.drinkhistory.dto.DrinkHistoryStatusDTO;

public interface DrinkHistoryQueryService {
    DrinkHistoryStatusDTO getDrinkRecordStatus(Long userId);
}
