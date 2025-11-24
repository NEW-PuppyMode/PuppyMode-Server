package com.umc.puppymode2.domain.puppy.service;

import com.umc.puppymode2.domain.goal.repository.UserGoalHistoryRepository;
import com.umc.puppymode2.domain.puppy.dto.MainResponseDto;
import com.umc.puppymode2.domain.puppy.entity.Puppy;
import com.umc.puppymode2.domain.puppy.entity.PuppyLevel;
import com.umc.puppymode2.domain.puppy.repository.PuppyRepository;
import com.umc.puppymode2.domain.drinkhistory.repository.DrinkHistoryRepository;
import com.umc.puppymode2.domain.user.entity.User;
import com.umc.puppymode2.domain.user.repository.UserRepository;
import com.umc.puppymode2.global.apiPayload.code.status.ErrorStatus;
import com.umc.puppymode2.global.auth.context.UserContext;
import com.umc.puppymode2.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class MainServiceImpl implements MainService {

    private final PuppyRepository puppyRepository;
    private final UserRepository userRepository;
    private final DrinkHistoryRepository drinkHistoryRepository;
    private final UserGoalHistoryRepository userGoalHistoryRepository;
    private final UserContext userContext;

    @Override
    public MainResponseDto getMainPageInfo() {
        Long userId = userContext.getCurrentUserId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        Puppy puppy = puppyRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.PUPPY_NOT_FOUND));

        PuppyLevel level = puppy.getPuppyLevel();

        int percent = calculateExp(puppy, level);

        boolean isPuppyName = puppy.isCustomName();
        boolean isMyName = user.isCustomName();

        boolean isGoal = userGoalHistoryRepository
                .findTopByUserIdOrderByGoalSetAtDesc(userId)
                .filter(h -> h.getGoalSetAt().getYear() == LocalDate.now().getYear()
                        && h.getGoalSetAt().getMonthValue() == LocalDate.now().getMonthValue())
                .isPresent();

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        boolean didRecordYesterday = drinkHistoryRepository.existsByUserUserIdAndDrinkDate(userId, yesterday);
        boolean didRecordToday = drinkHistoryRepository.existsByUserUserIdAndDrinkDate(userId, today);

        return MainResponseDto.builder()
                .puppyLevel(level.getPuppyLevel())
                .puppyLevelName(level.getLevelName())
                .puppyLevelPercent(percent)
                .puppyImageUrl(level.getLevelImageUrl())
                .currentPuppyName(puppy.getPuppyName())
                .puppyName(isPuppyName)
                .myName(isMyName)
                .goal(isGoal)
                .didRecordYesterday(didRecordYesterday)
                .didRecordToday(didRecordToday)
                .build();
    }

    private int calculateExp(Puppy puppy, PuppyLevel level) {
        long currentExp = puppy.getPuppyExp();
        long minExp = level.getLevelMinExp();
        long maxExp = level.getLevelMaxExp();

        double ratio = (double) (currentExp - minExp) / (double) (maxExp - minExp);
        int percent = (int) Math.round(ratio * 100);
        if (percent < 0) percent = 0;
        else if (percent > 100) percent = 100;

        return percent;
    }

}
