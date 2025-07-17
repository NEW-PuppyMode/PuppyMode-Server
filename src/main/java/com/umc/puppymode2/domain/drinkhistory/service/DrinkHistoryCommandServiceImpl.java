package com.umc.puppymode2.domain.drinkhistory.service;

import com.umc.puppymode2.domain.drinkhistory.converter.DrinkHistoryConverter;
import com.umc.puppymode2.domain.drinkhistory.dto.DrinkHistoryRequestDTO;
import com.umc.puppymode2.domain.drinkhistory.entity.DrinkHistory;
import com.umc.puppymode2.domain.drinkhistory.repository.DrinkHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@Transactional
@RequiredArgsConstructor
public class DrinkHistoryCommandServiceImpl implements DrinkHistoryCommandService {

    private final DrinkHistoryRepository drinkHistoryRepository;

    @Override
    public Long recordDrink(Long userId, DrinkHistoryRequestDTO.CreateDrinkHistory dto) {
        boolean alreadyExists = drinkHistoryRepository.existsByUserIdAndDrinkDate(userId, dto.getDrinkDate());
        if (alreadyExists) {
            throw new IllegalStateException("이미 해당 날짜에 음주 기록이 존재합니다.");
        }
        DrinkHistory drinkHistory = drinkHistoryRepository.save(DrinkHistoryConverter.toEntity(userId, dto));
        return drinkHistory.getDrinkHistoryId();
    }
}
