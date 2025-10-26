package com.umc.puppymode2.domain.drinkhistory.service;

import com.umc.puppymode2.domain.drinkhistory.converter.DrinkHistoryConverter;
import com.umc.puppymode2.domain.drinkhistory.dto.DrinkHistoryRequestDTO;
import com.umc.puppymode2.domain.drinkhistory.entity.DrinkHistory;
import com.umc.puppymode2.domain.drinkhistory.repository.DrinkHistoryRepository;
import com.umc.puppymode2.domain.puppy.entity.Puppy;
import com.umc.puppymode2.domain.puppy.repository.PuppyRepository;
import com.umc.puppymode2.domain.user.entity.User;
import com.umc.puppymode2.domain.user.repository.UserRepository;
import com.umc.puppymode2.global.apiPayload.code.status.ErrorStatus;
import com.umc.puppymode2.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class DrinkHistoryCommandServiceImpl implements DrinkHistoryCommandService {
    private final UserRepository userRepository;
    private final DrinkHistoryRepository drinkHistoryRepository;
    private final PuppyRepository puppyRepository;

    @Override
    public Long recordDrink(Long userId, DrinkHistoryRequestDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        boolean alreadyExists = drinkHistoryRepository.existsByUserUserIdAndDrinkDate(userId, dto.getDrinkDate());
        if (alreadyExists) {
            throw new IllegalStateException("이미 해당 날짜에 음주 기록이 존재합니다.");
        }
        DrinkHistory drinkHistory = drinkHistoryRepository.save(DrinkHistoryConverter.toEntity(user, dto));

        Puppy puppy = puppyRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.PUPPY_NOT_FOUND));
        int updatedExp = puppy.getPuppyExp() + 10;
        puppy.setPuppyExp(updatedExp);

        puppyRepository.save(puppy);

        return drinkHistory.getDrinkHistoryId();
    }
}
