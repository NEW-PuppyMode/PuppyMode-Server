package com.umc.puppymode2.domain.goal.service;

import com.umc.puppymode2.domain.goal.converter.UserGoalHistoryConverter;
import com.umc.puppymode2.domain.goal.dto.GoalPostRequestDTO;
import com.umc.puppymode2.domain.goal.dto.GoalPostResponseDTO;
import com.umc.puppymode2.domain.goal.entity.UserGoalHistory;
import com.umc.puppymode2.domain.goal.repository.UserGoalHistoryRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserGoalHistoryCommandServiceImpl implements UserGoalHistoryCommandService {

    private final UserGoalHistoryRepository repository;
    private final UserGoalHistoryConverter converter;

    @Transactional
    @Override
    public GoalPostResponseDTO postGoal(Long userId, GoalPostRequestDTO dto) {

        LocalDate now = LocalDate.now();
        LocalDate goalMonth = now.withDayOfMonth(1);
        int maxGoal = now.lengthOfMonth();

        try {

            // 새로운 목표 설정
            if (Boolean.TRUE.equals(dto.getIsNew())) {

                if (dto.getGoal() == null) {
                    throw new IllegalArgumentException("새로운 목표 설정 시 goal 값은 필수입니다.");
                }

                if (dto.getGoal() <= 0 || dto.getGoal() > maxGoal) {
                    throw new IllegalArgumentException("목표는 1 이상 " + maxGoal + " 이하여야 합니다.");
                }

                if (repository.existsByUserIdAndGoalMonth(userId, goalMonth)) {
                    throw new IllegalStateException("이미 이번 달 목표가 설정되어 있습니다.");
                }

                UserGoalHistory newGoal = converter.toEntity(dto, userId);

                repository.save(newGoal);

                return GoalPostResponseDTO.builder()
                        .isSuccess(true)
                        .code("POST_GOAL_SUCCESS")
                        .message("목표 설정 성공")
                        .build();
            }

            // 기존 목표 유지
            UserGoalHistory lastGoal = repository.findTopByUserIdOrderByGoalMonthDesc(userId)
                    .orElseThrow(() -> new IllegalArgumentException("기존 목표가 없습니다."));

            if (repository.existsByUserIdAndGoalMonth(userId, goalMonth)) {
                throw new IllegalStateException("이미 이번 달 목표가 설정되어 있습니다.");
            }

            int adjustedGoal = Math.min(lastGoal.getMonthlyGoalCount(), maxGoal);

            UserGoalHistory copiedGoal = UserGoalHistory.builder()
                    .userId(userId)
                    .goalMonth(goalMonth)
                    .monthlyGoalCount(adjustedGoal)
                    .goalSetAt(LocalDateTime.now())
                    .build();

            repository.save(copiedGoal);

            return GoalPostResponseDTO.builder()
                    .isSuccess(true)
                    .code("MAINTAIN_GOAL_SUCCESS")
                    .message("기존 목표 유지 성공")
                    .build();

        } catch (DataIntegrityViolationException e) {

            // DB UNIQUE(user_id, goal_month) 제약 위반
            throw new IllegalStateException("이미 이번 달 목표가 설정되어 있습니다.");
        }
    }
}