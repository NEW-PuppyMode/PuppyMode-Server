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

            if (dto.getGoal() == null) {
                throw new IllegalArgumentException("새로운 목표 설정 시 goal 값은 필수입니다.");
            }
            if (dto.getGoal() < 0 || dto.getGoal() > 30) {
                throw new IllegalArgumentException("목표는 0 이상 30 이하여야 합니다.");
            }

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
                    .orElseThrow(() -> new IllegalArgumentException("기존 목표가 없습니다."));
            UserGoalHistory copiedGoal = UserGoalHistory.builder()
                    .userId(userId)
                    .monthlyGoalCount(lastGoal.getMonthlyGoalCount())
                    .monthlyActualCount(0)
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

