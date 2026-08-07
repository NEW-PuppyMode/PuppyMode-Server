package com.umc.puppymode2.domain.puppy.service;

import com.umc.puppymode2.domain.goal.repository.UserGoalHistoryRepository;
import com.umc.puppymode2.domain.puppy.dto.MainResponseDto;
import com.umc.puppymode2.domain.puppy.entity.*;
import com.umc.puppymode2.domain.puppy.repository.LevelExpRepository;
import com.umc.puppymode2.domain.puppy.repository.PuppyAppearanceRepository;
import com.umc.puppymode2.domain.puppy.repository.PuppyRepository;
import com.umc.puppymode2.domain.drinkhistory.repository.DrinkHistoryRepository;
import com.umc.puppymode2.domain.user.entity.User;
import com.umc.puppymode2.domain.user.repository.UserRepository;
import com.umc.puppymode2.global.apiPayload.code.status.ErrorStatus;
import com.umc.puppymode2.global.auth.context.UserContext;
import com.umc.puppymode2.global.exception.GeneralException;
import com.umc.puppymode2.global.util.TimeConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MainServiceImpl implements MainService {

    private final PuppyRepository puppyRepository;
    private final UserRepository userRepository;
    private final DrinkHistoryRepository drinkHistoryRepository;
    private final UserGoalHistoryRepository userGoalHistoryRepository;
    private final LevelExpRepository levelExpRepository;
    private final UserContext userContext;
    private final PuppyAppearanceRepository puppyAppearanceRepository;

    @Override
    public MainResponseDto getMainPageInfo() {
        Long userId = userContext.getCurrentUserId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        Optional<Puppy> puppyOpt = puppyRepository.findByUser_UserId(userId);

        // 강아지 유형 테스트 미완료
        if (puppyOpt.isEmpty()) {
            return MainResponseDto.builder()
                    .onboarded(false)
                    .breedTestDone(false)
                    .build();
        }

        // 강아지 유형 테스트 완료
        Puppy puppy = puppyOpt.get();

        LevelExp levelInfo = levelExpRepository.findByExp(puppy.getPuppyExp())
                .orElseThrow(() -> new GeneralException(ErrorStatus.LEVEL_NOT_FOUND));
        int currentLevel = levelInfo.getLevel();
        int percent = calculateExp(puppy.getPuppyExp(), levelInfo);
        PuppyAppearance appearance = getAppearance(puppy.getPuppyType(), currentLevel);

        boolean isPuppyName = puppy.isCustomName();
        boolean isMyName = user.isCustomName();

        LocalDate today = LocalDate.now(TimeConstants.KST);

        boolean isGoal = userGoalHistoryRepository
                .findTopByUserIdOrderByGoalSetAtDesc(userId)
                .filter(h -> h.getGoalSetAt().getYear() == today.getYear()
                        && h.getGoalSetAt().getMonthValue() == today.getMonthValue())
                .isPresent();

        LocalDate yesterday = today.minusDays(1);
        boolean didRecordYesterday = drinkHistoryRepository.existsByUserUserIdAndDrinkDate(userId, yesterday);
        boolean didRecordToday = drinkHistoryRepository.existsByUserUserIdAndDrinkDate(userId, today);

        return MainResponseDto.builder()
                .onboarded(true)
                .breedTestDone(true)
                .puppyLevel(currentLevel)
                .puppyLevelName(appearance.getStageName())
                .puppyLevelPercent(percent)
                .puppyImageUrl(appearance.getImageUrl())
                .currentPuppyName(puppy.getPuppyName())
                .puppyName(isPuppyName)
                .myName(isMyName)
                .goal(isGoal)
                .didRecordYesterday(didRecordYesterday)
                .didRecordToday(didRecordToday)
                .build();
    }

    // 레벨 단위 진행률 계산
    // 현재 레벨의 minExp ~ maxExp 구간 기준
    // 레벨 30 도달 시 100% 고정
    private int calculateExp(int exp, LevelExp levelInfo) {
        if (levelInfo.getLevel() == 30) return 100;

        int minExp = levelInfo.getMinExp();
        int maxExp = levelInfo.getMaxExp();

        double ratio = (double) (exp - minExp) / (maxExp - minExp);
        return (int) (ratio * 100);
    }

    private PuppyAppearance getAppearance(PuppyType type, int level) {
        return puppyAppearanceRepository.findByPuppyTypeAndLevel(type, level)
                .orElseThrow(() -> new GeneralException(ErrorStatus.APPEARANCE_NOT_FOUND));
    }

}