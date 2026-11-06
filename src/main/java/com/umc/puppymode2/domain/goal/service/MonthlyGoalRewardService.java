package com.umc.puppymode2.domain.goal.service;


import com.umc.puppymode2.domain.drinkhistory.repository.DrinkHistoryRepository;
import com.umc.puppymode2.domain.drinkhistory.repository.UserDrinkCountProjection;
import com.umc.puppymode2.domain.goal.dto.MonthlyGoalRewardResponseDTO;
import com.umc.puppymode2.domain.goal.entity.UserGoalHistory;
import com.umc.puppymode2.domain.goal.repository.UserGoalHistoryRepository;
import com.umc.puppymode2.domain.puppy.entity.Puppy;
import com.umc.puppymode2.domain.puppy.repository.PuppyRepository;
import com.umc.puppymode2.domain.puppy.service.PuppyLevelCalculator;
import com.umc.puppymode2.domain.report.dto.GoalRewardDTO;
import com.umc.puppymode2.domain.report.dto.LevelSnapshotDTO;
import com.umc.puppymode2.global.apiPayload.code.status.ErrorStatus;
import com.umc.puppymode2.global.cache.DrinkReportCacheService;
import com.umc.puppymode2.global.exception.GeneralException;
import com.umc.puppymode2.global.util.TimeConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MonthlyGoalRewardService {

    private static final long MIN_RECORD_DAYS = 20L;
    private static final int MONTHLY_REWARD_EXP = 100;

    private final UserGoalHistoryRepository userGoalHistoryRepository;
    private final PuppyLevelCalculator puppyLevelCalculator;
    private final DrinkHistoryRepository drinkHistoryRepository;
    private final PuppyRepository puppyRepository;
    private final DrinkReportCacheService reportCacheService;

    @Transactional
    public MonthlyGoalRewardResponseDTO grantReward(Long userId, YearMonth month) {
        // 1. 지난 달만 판정 가능
        if (!month.isBefore(YearMonth.now(TimeConstants.KST))) {
            throw new GeneralException(ErrorStatus.GOAL_REWARD_MONTH_NOT_ENDED);
        }

        // 2. 목표 조회 + 행 장금
        Optional<UserGoalHistory> goalOpt = userGoalHistoryRepository
                .findForUpdate(userId, month.atDay(1));
        if (goalOpt.isEmpty()) {
            return notRewarded();
        }
        UserGoalHistory goal = goalOpt.get();

        // 3. 이미 받은 달
        if (goal.isRewarded()) {
            return rewarded(goal.getRewardExpBefore(), goal.getRewardExpAfter());
        }

        // 4. 해당 월 기록 집계
        LocalDate firstDay = month.atDay(1);
        LocalDate lastDay = month.atEndOfMonth();
        List<UserDrinkCountProjection> stats = drinkHistoryRepository
                .countMonthlyDrinkByUserIds(List.of(userId), firstDay, lastDay);
        if (stats.isEmpty()) {
            return notRewarded();
        }
        UserDrinkCountProjection stat = stats.get(0);

        // 5. 조건: 20일 이상 기록 + 음주 횟수 <= 목표
        long recordDays = stat.getRecordDays() == null ? 0L : stat.getRecordDays();
        long drinkCount = stat.getDrinkCount() == null ? 0L : stat.getDrinkCount();
        if (recordDays < MIN_RECORD_DAYS || drinkCount > goal.getMonthlyGoalCount()) {
            return notRewarded();
        }

        // 6. 지급
        Puppy puppy = puppyRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.PUPPY_NOT_FOUND));

        int expBefore = puppy.getPuppyExp();
        int expAfter = expBefore + MONTHLY_REWARD_EXP;
        puppy.setPuppyExp(expAfter);
        goal.markRewarded(expBefore,  expAfter);

        // 캐시무효화
        reportCacheService.evict(userId, month);

        return rewarded(expBefore, expAfter);
    }

    private MonthlyGoalRewardResponseDTO notRewarded() {
        return MonthlyGoalRewardResponseDTO.builder()
                .rewarded(false)
                .goalReward(null)
                .build();
    }

    private MonthlyGoalRewardResponseDTO rewarded(Integer expBefore, Integer expAfter) {
        GoalRewardDTO goalReward = (expBefore == null || expAfter == null)
                ? null
                : GoalRewardDTO.builder()
                .before(toLevelSnapshot(expBefore))
                .after(toLevelSnapshot(expAfter))
                .build();

        return MonthlyGoalRewardResponseDTO.builder()
                .rewarded(true)
                .goalReward(goalReward)
                .build();
    }

    private LevelSnapshotDTO toLevelSnapshot(int exp) {
        PuppyLevelCalculator.LevelProgress progress = puppyLevelCalculator.calculate(exp);
        return LevelSnapshotDTO.builder()
                .level(progress.level())
                .percent(progress.percent())
                .build();
    }


}
