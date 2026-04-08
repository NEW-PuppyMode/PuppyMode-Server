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

        // 온보딩 미완료
        if (puppyOpt.isEmpty()) {
            return MainResponseDto.builder()
                    .isOnboarded(false)
                    .build();
        }

        // 온보딩 완료
        Puppy puppy = puppyOpt.get();
        int currentLevel = getCurrentLevel(puppy.getPuppyExp());
        int percent = calculateExp(puppy.getPuppyExp());
        PuppyAppearance appearance = getAppearance(puppy.getPuppyType(), currentLevel);

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
                .isOnboarded(true)
                .puppyLevel(appearance.getStage())
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

    private int getCurrentLevel(int exp) {
        return levelExpRepository.findByExp(exp)
                .map(LevelExp::getLevel)
                .orElseThrow(() -> new GeneralException(ErrorStatus.LEVEL_NOT_FOUND));
    }

    // 외형 단계 내 진행도 퍼센트 계산
    // Stage 1: exp 0~269 (레벨 1~9)
    // Stage 2: exp 270~1044 (레벨 10~19)
    // Stage 3: exp 1045~2044 (레벨 20~29)
    // 레벨 30 도달 시 100% 고정
    private int calculateExp(int exp) {
        LevelExp levelInfo = levelExpRepository.findByExp(exp)
                .orElseThrow(() -> new GeneralException(ErrorStatus.LEVEL_NOT_FOUND));
        int currentLevel = levelInfo.getLevel();
        if (currentLevel == 30) return 100;
        int stageMinExp = currentLevel < 10 ? 0 : currentLevel < 20 ? 270 : 1045;
        int stageMaxExp = currentLevel < 10 ? 270 : currentLevel < 20 ? 1045 : 2045;
        double ratio = (double) (exp - stageMinExp) / (stageMaxExp - stageMinExp);
        return (int) (ratio * 100);
    }

    private PuppyAppearance getAppearance(PuppyType type, int level) {
        return puppyAppearanceRepository.findByPuppyTypeAndLevel(type, level)
                .orElseThrow(() -> new GeneralException(ErrorStatus.APPEARANCE_NOT_FOUND));
    }

}
