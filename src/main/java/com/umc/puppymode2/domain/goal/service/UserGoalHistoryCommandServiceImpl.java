package com.umc.puppymode2.domain.goal.service;

import com.umc.puppymode2.domain.goal.converter.UserGoalHistoryConverter;
import com.umc.puppymode2.domain.goal.dto.GoalPostRequestDTO;
import com.umc.puppymode2.domain.goal.dto.GoalPostResponseDTO;
import com.umc.puppymode2.domain.goal.entity.UserGoalHistory;
import com.umc.puppymode2.domain.goal.repository.UserGoalHistoryRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserGoalHistoryCommandServiceImpl implements UserGoalHistoryCommandService {
    private final UserGoalHistoryRepository repository;
    private final UserGoalHistoryConverter converter;

    @Transactional
    @Override
    public GoalPostResponseDTO postGoal(Long userId, GoalPostRequestDTO dto) {
        if (Boolean.TRUE.equals(dto.getIsNew())) {
            UserGoalHistory newGoal = converter.toEntity(dto, userId);
            repository.save(newGoal);

            return GoalPostResponseDTO.builder()
                    .isSuccess(true)
                    .code("POST_GOAL_SUCCESS")
                    .message("목표 설정 성공")
                    .build();
        } else {
            // 기존 목표 유지
            UserGoalHistory lastGoal = repository.findTopByUserIdOrderByGoalSetAtDesc(userId)
                    .orElseThrow(() -> new IllegalStateException("기존 목표가 없습니다."));
            UserGoalHistory copiedGoal = UserGoalHistory.builder()
                    .userId(userId)
                    .monthlyGoalCount(lastGoal.getMonthlyGoalCount())
                    .monthlyActualCount(0)
                    .isGoalExceeded(false)
                    .goalSetAt(LocalDateTime.now())
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

