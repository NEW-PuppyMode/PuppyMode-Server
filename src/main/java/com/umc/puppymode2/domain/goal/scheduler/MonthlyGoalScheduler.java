package com.umc.puppymode2.domain.goal.scheduler;

import com.umc.puppymode2.domain.drinkhistory.repository.DrinkHistoryRepository;
import com.umc.puppymode2.domain.drinkhistory.repository.UserDrinkCountProjection;
import com.umc.puppymode2.domain.goal.entity.UserGoalHistory;
import com.umc.puppymode2.domain.goal.repository.UserGoalHistoryRepository;
import com.umc.puppymode2.domain.puppy.entity.Puppy;
import com.umc.puppymode2.domain.puppy.repository.PuppyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MonthlyGoalScheduler {

    private final UserGoalHistoryRepository userGoalHistoryRepository;
    private final DrinkHistoryRepository drinkHistoryRepository;
    private final PuppyRepository puppyRepository;

    /**
     * 매월 마지막 날 23:59에 실행되는 월간 목표 달성 보상 스케줄러
     * 이번 달 음주 기록이 있고 실제 음주 횟수가 목표 이하인 유저에게 경험치 300 지급
     */
    @Scheduled(cron = "0 59 23 L * *", zone = "Asia/Seoul")
    @Transactional
    public void evaluateMonthlyGoals() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        LocalDate firstDay = today.withDayOfMonth(1);
        LocalDate lastDay = today.withDayOfMonth(today.lengthOfMonth());

        // 이번 달 목표가 있고 아직 보상을 받지 않은 유저 목록 조회
        List<UserGoalHistory> goals = userGoalHistoryRepository
                .findAllByGoalMonthAndRewardedFalse(firstDay);

        if (goals.isEmpty()) return;

        List<Long> userIds = goals.stream()
                .map(UserGoalHistory::getUserId)
                .distinct()
                .collect(Collectors.toList());

        // 보상 대상 유저들의 강아지 조회
        Map<Long, Puppy> puppyMap = puppyRepository.findAllByUserUserIdIn(userIds)
                .stream()
                .collect(Collectors.toMap(
                        p -> p.getUser().getUserId(),
                        Function.identity()
                ));

        // 보상 대상 유저들의 이번 달 음주 기록 통계 조회
        Map<Long, UserDrinkCountProjection> drinkStatMap = drinkHistoryRepository
                .countMonthlyDrinkByUserIds(userIds, firstDay, lastDay)
                .stream()
                .collect(Collectors.toMap(
                        UserDrinkCountProjection::getUserId,
                        Function.identity()
                ));

        for (UserGoalHistory goal : goals) {
            Long userId = goal.getUserId();
            UserDrinkCountProjection stat = drinkStatMap.get(userId);

            // 목표만 세우고 기록 안 한 경우 경험치 미지급
            if (stat == null || stat.getTotalCount() == 0) continue;

            long drinkCount = stat.getDrinkCount() == null ? 0L : stat.getDrinkCount();

            // 이번 달 음주 횟수가 목표 이하면 달성 - 경험치 지급
            if (drinkCount <= goal.getMonthlyGoalCount()) {
                Puppy puppy = puppyMap.get(userId);
                if (puppy == null) continue;

                puppy.setPuppyExp(puppy.getPuppyExp() + 300);
                goal.markRewarded();
            }
        }
    }
}