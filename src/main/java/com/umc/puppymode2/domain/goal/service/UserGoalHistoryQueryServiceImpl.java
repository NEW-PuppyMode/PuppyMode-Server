package com.umc.puppymode2.domain.goal.service;

import com.umc.puppymode2.domain.drinkhistory.repository.DrinkHistoryRepository;
import com.umc.puppymode2.domain.goal.converter.UserGoalHistoryConverter;
import com.umc.puppymode2.domain.goal.dto.GoalInfoResponseDTO;
import com.umc.puppymode2.domain.goal.entity.UserGoalHistory;
import com.umc.puppymode2.domain.goal.repository.UserGoalHistoryRepository;
import com.umc.puppymode2.global.util.TimeConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserGoalHistoryQueryServiceImpl implements UserGoalHistoryQueryService {

    private final UserGoalHistoryRepository repository;
    private final UserGoalHistoryConverter converter;
    private final DrinkHistoryRepository drinkHistoryRepository;

    @Override
    public GoalInfoResponseDTO getLatestGoal(Long userId) {

        LocalDate now = LocalDate.now(TimeConstants.KST);
        LocalDate goalMonth = now.withDayOfMonth(1); // 이번 달 기준

        // 이번 달 목표 조회 (없을 수 있음)
        UserGoalHistory currentMonthGoal = repository
                .findByUserIdAndGoalMonth(userId, goalMonth)
                .orElse(null);

        // 가장 최근에 설정한 목표 조회.
        // 이번 달 목표가 있으면 보통 그것과 같지만, 이번 달 목표가 없을 때는 과거의 마지막 목표를 가리킨다.
        // 프론트는 이 연/월로 월간 리포트(GET /report?year&month)를 호출한다.
        // 목표 이력이 전혀 없는 신규 유저는 null.
        UserGoalHistory latestGoal = repository
                .findTopByUserIdOrderByGoalMonthDesc(userId)
                .orElse(null);

        // 이번 달 목표가 있을 때만 이번 달 음주 횟수를 계산한다. (없으면 불필요한 count 쿼리를 아낀다)
        Long actualCount = null;
        if (currentMonthGoal != null) {
            LocalDate firstDay = goalMonth;
            LocalDate lastDay = goalMonth.withDayOfMonth(goalMonth.lengthOfMonth());

            actualCount = drinkHistoryRepository
                    .countByUserUserIdAndIsDrinkTrueAndDrinkDateBetween(userId, firstDay, lastDay);
        }

        // 이전에는 이번 달 목표가 없으면 null을 반환했지만, 이제는 항상 DTO를 반환한다.
        // (이번 달 목표가 없어도 latestGoalYear/Month는 내려줘야 하기 때문)
        return converter.toDto(currentMonthGoal, actualCount, latestGoal);
    }

    @Override
    public List<String> getGoalMonths(Long userId) {

        // goalMonth는 항상 해당 월의 1일(LocalDate)로 저장된다.
        // YearMonth로 변환하면 toString()이 그대로 "2026-06" 형태의 문자열을 만들어 주므로
        // 별도 포맷터 없이 프론트가 원하는 형식으로 내려줄 수 있다.
        return repository.findGoalMonthsByUserId(userId).stream()
                .map(goalMonth -> YearMonth.from(goalMonth).toString())
                .toList();
    }

    @Override
    public boolean isMoreThan30DayPassed(Long userId) {

        LocalDate goalMonth = LocalDate.now(TimeConstants.KST).withDayOfMonth(1);

        // 이번 달 목표 존재 여부 확인
        return repository.findByUserIdAndGoalMonth(userId, goalMonth).isEmpty();
    }
}