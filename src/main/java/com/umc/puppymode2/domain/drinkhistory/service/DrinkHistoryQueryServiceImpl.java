package com.umc.puppymode2.domain.drinkhistory.service;

import com.umc.puppymode2.domain.drinkhistory.converter.DrinkHistoryConverter;
import com.umc.puppymode2.domain.drinkhistory.dto.DrinkHistoryStatusDTO;
import com.umc.puppymode2.domain.drinkhistory.repository.DrinkHistoryRepository;
import com.umc.puppymode2.global.util.TimeConstants;
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
    public DrinkHistoryStatusDTO getDrinkRecordStatus(Long userId) {
        LocalDate today = LocalDate.now(TimeConstants.KST);
        LocalDate yesterday = today.minusDays(1);

        boolean hasToday = drinkHistoryRepository.existsByUserUserIdAndDrinkDate(userId, today);
        boolean hasYesterday = drinkHistoryRepository.existsByUserUserIdAndDrinkDate(userId, yesterday);

        return DrinkHistoryConverter.toStatusDTO(hasYesterday, hasToday);
    }
}