package com.umc.puppymode2.domain.drinkhistory.service;

import com.umc.puppymode2.domain.drinkhistory.converter.DrinkHistoryConverter;
import com.umc.puppymode2.domain.drinkhistory.dto.DrinkHistoryRequestDTO;
import com.umc.puppymode2.domain.drinkhistory.dto.DrinkHistoryResponseDTO;
import com.umc.puppymode2.domain.drinkhistory.entity.DrinkHistory;
import com.umc.puppymode2.domain.drinkhistory.repository.DrinkHistoryRepository;
import com.umc.puppymode2.domain.puppy.entity.Puppy;
import com.umc.puppymode2.domain.puppy.repository.PuppyRepository;
import com.umc.puppymode2.domain.user.entity.User;
import com.umc.puppymode2.domain.user.repository.UserRepository;
import com.umc.puppymode2.global.apiPayload.code.status.ErrorStatus;
import com.umc.puppymode2.global.cache.DrinkReportCacheService;
import com.umc.puppymode2.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;

@Service
@Transactional
@RequiredArgsConstructor
public class DrinkHistoryCommandServiceImpl implements DrinkHistoryCommandService {
    private final UserRepository userRepository;
    private final DrinkHistoryRepository drinkHistoryRepository;
    private final PuppyRepository puppyRepository;
    private final DrinkReportCacheService reportCacheService;

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

        puppy.setPuppyExp(puppy.getPuppyExp() + 10);

        // 음주 기록이 바뀌면 해당 월 리포트(음주 횟수/일수)가 달라지므로
        // 캐시된 리포트를 무효화한다. dto.getDrinkDate() 기준이라 과거 날짜를
        // 백필해도 정확히 그 달의 캐시만 지워진다.
        reportCacheService.evict(userId, YearMonth.from(dto.getDrinkDate()));

        return DrinkHistoryConverter.toResponseDTO(drinkHistory.getDrinkHistoryId(), puppy.getPuppyExp());
    }
}
