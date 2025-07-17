package com.umc.puppymode2.domain.drinkhistory.service;

import com.umc.puppymode2.domain.drinkhistory.converter.DrinkHistoryConverter;
import com.umc.puppymode2.domain.drinkhistory.dto.DrinkHistoryResponseDTO;
import com.umc.puppymode2.domain.drinkhistory.repository.DrinkHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@Transactional
@RequiredArgsConstructor
public class DrinkHistoryQueryServiceImpl implements DrinkHistoryQueryService {
    private final DrinkHistoryRepository drinkHistoryRepository;
    @Override
    public DrinkHistoryResponseDTO.DrinkStatus getDrinkRecordStatus(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        boolean hasToday = drinkHistoryRepository.existsByUserIdAndDrinkDate(userId, today);
        boolean hasYesterday = drinkHistoryRepository.existsByUserIdAndDrinkDate(userId, yesterday);

        return DrinkHistoryConverter.toStatusDTO(hasYesterday, hasToday);
    }
}
