package com.umc.puppymode2.domain.goal.service;

import com.umc.puppymode2.domain.goal.converter.UserGoalHistoryConverter;
import com.umc.puppymode2.domain.goal.dto.GoalInfoResponseDTO;
import com.umc.puppymode2.domain.goal.repository.UserGoalHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class UserGoalHistoryQueryServiceImpl implements UserGoalHistoryQueryService {
    private final UserGoalHistoryRepository repository;
    private final UserGoalHistoryConverter converter;


    @Override
    public GoalInfoResponseDTO getLatestGoal(Long userId) {
        return repository.findTopByUserIdOrderByGoalSetAtDesc(userId)
                .map(converter::toDto)
                .orElse(null);
    }

    @Override
    public boolean isMoreThan30DayPassed(Long userId) {
        return repository.findTopByUserIdOrderByGoalSetAtDesc(userId)
                .map(goal -> ChronoUnit.DAYS.between(goal.getGoalSetAt(), LocalDateTime.now()) >= 30)
                .orElse(true);

    }
}
