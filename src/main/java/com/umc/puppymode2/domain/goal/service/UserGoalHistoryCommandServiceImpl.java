package com.umc.puppymode2.domain.goal.service;

import com.umc.puppymode2.domain.drinkhistory.repository.DrinkHistoryRepository;
import com.umc.puppymode2.domain.goal.converter.UserGoalHistoryConverter;
import com.umc.puppymode2.domain.goal.dto.GoalPostRequestDTO;
import com.umc.puppymode2.domain.goal.dto.GoalPostResponseDTO;
import com.umc.puppymode2.domain.goal.entity.UserGoalHistory;
import com.umc.puppymode2.domain.goal.repository.UserGoalHistoryRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserGoalHistoryCommandServiceImpl implements UserGoalHistoryCommandService {
    private final UserGoalHistoryRepository repository;
    private final UserGoalHistoryConverter converter;
    private final DrinkHistoryRepository drinkHistoryRepository;

    private static final int MAX_GOAL = 31;

    @Transactional
    @Override
    public GoalPostResponseDTO postGoal(Long userId, GoalPostRequestDTO dto) {

        LocalDate now = LocalDate.now();
        LocalDate firstDayOfMonth = now.withDayOfMonth(1);
        LocalDate lastDayOfMonth = now.withDayOfMonth(now.lengthOfMonth());

        Long actualDrinkCount = drinkHistoryRepository.countByUserUserIdAndDrinkDateBetween(userId, firstDayOfMonth, lastDayOfMonth);

        if (Boolean.TRUE.equals(dto.getIsNew())) {

            if (dto.getGoal() == null) {
                throw new IllegalArgumentException("새로운 목표 설정 시 goal 값은 필수입니다.");
            }

            // 기준 : 오늘부터 30일의 목표
            if (dto.getGoal() < 0 || dto.getGoal() > MAX_GOAL) {
                throw new IllegalArgumentException("목표는 0 이상 " + MAX_GOAL + " 이하여야 합니다.");
            }

            UserGoalHistory newGoal = converter.toEntity(dto, userId, actualDrinkCount);
            repository.save(newGoal);

            return GoalPostResponseDTO.builder()
                    .isSuccess(true)
                    .code("POST_GOAL_SUCCESS")
                    .message("목표 설정 성공")
                    .build();
        } else {
            // 기존 목표 유지
            UserGoalHistory lastGoal = repository.findTopByUserIdOrderByGoalSetAtDesc(userId)
                    .orElseThrow(() -> new IllegalArgumentException("기존 목표가 없습니다."));
            UserGoalHistory copiedGoal = UserGoalHistory.builder()
                    .userId(userId)
                    .monthlyGoalCount(lastGoal.getMonthlyGoalCount())
                    .monthlyActualCount(actualDrinkCount)
                    .isGoalExceeded(false)
                    .goalSetAt(LocalDateTime.now())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            repository.save(copiedGoal);

            return GoalPostResponseDTO.builder()
                    .isSuccess(true)
                    .code("MAINTAIN_GOAL_SUCCESS")
                    .message("기존 목표 유지 성공")
                    .build();
        }
    }
}

