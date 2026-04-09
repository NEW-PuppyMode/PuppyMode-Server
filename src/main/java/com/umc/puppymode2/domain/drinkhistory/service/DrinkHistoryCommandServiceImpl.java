package com.umc.puppymode2.domain.drinkhistory.service;

import com.umc.puppymode2.domain.drinkhistory.converter.DrinkHistoryConverter;
import com.umc.puppymode2.domain.drinkhistory.dto.DrinkHistoryRequestDTO;
import com.umc.puppymode2.domain.drinkhistory.dto.DrinkHistoryResponseDTO;
import com.umc.puppymode2.domain.drinkhistory.entity.DrinkHistory;
import com.umc.puppymode2.domain.drinkhistory.repository.DrinkHistoryRepository;
import com.umc.puppymode2.domain.goal.entity.UserGoalHistory;
import com.umc.puppymode2.domain.goal.repository.UserGoalHistoryRepository;
import com.umc.puppymode2.domain.puppy.entity.Puppy;
import com.umc.puppymode2.domain.puppy.repository.PuppyRepository;
import com.umc.puppymode2.domain.user.entity.User;
import com.umc.puppymode2.domain.user.repository.UserRepository;
import com.umc.puppymode2.global.apiPayload.code.status.ErrorStatus;
import com.umc.puppymode2.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@Transactional
@RequiredArgsConstructor
public class DrinkHistoryCommandServiceImpl implements DrinkHistoryCommandService {
    private final UserRepository userRepository;
    private final DrinkHistoryRepository drinkHistoryRepository;
    private final PuppyRepository puppyRepository;
    private final UserGoalHistoryRepository userGoalHistoryRepository;

    @Override
    public DrinkHistoryResponseDTO recordDrink(Long userId, DrinkHistoryRequestDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        boolean alreadyExists = drinkHistoryRepository.existsByUserUserIdAndDrinkDate(userId, dto.getDrinkDate());
        if (alreadyExists) {
            throw new IllegalStateException("이미 해당 날짜에 음주 기록이 존재합니다.");
        }
        DrinkHistory drinkHistory = drinkHistoryRepository.save(DrinkHistoryConverter.toEntity(user, dto));

        Puppy puppy = puppyRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.PUPPY_NOT_FOUND));

        // 음주 기록 경험치
        int drinkExp = 10;

        // 월간 목표 달성 여부 체크 -> 달성시 300 exp 추가 지급 (중복 지급x)
        LocalDate today = dto.getDrinkDate();
        LocalDate firstDay = today.withDayOfMonth(1);
        LocalDate lastDay = today.withDayOfMonth(today.lengthOfMonth());

        UserGoalHistory goal = userGoalHistoryRepository.findByUserIdAndGoalMonth(userId, firstDay).orElse(null);

        if (goal != null && !goal.isRewarded() && Boolean.FALSE.equals(dto.getIsDrink())) {
            long nonDrinkCount = drinkHistoryRepository
                    .countByUserUserIdAndIsDrinkFalseAndDrinkDateBetween(userId, firstDay, lastDay);

            if (nonDrinkCount >= goal.getMonthlyGoalCount()) {
                drinkExp += 300;
                goal.markRewarded();
            }
        }

        puppy.setPuppyExp(puppy.getPuppyExp() + drinkExp);

        puppyRepository.save(puppy);

        return DrinkHistoryConverter.toResponseDTO(drinkHistory.getDrinkHistoryId(), puppy.getPuppyExp());
    }
}
