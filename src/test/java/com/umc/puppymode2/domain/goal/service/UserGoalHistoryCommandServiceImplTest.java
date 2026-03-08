package com.umc.puppymode2.domain.goal.service;

import com.umc.puppymode2.domain.goal.converter.UserGoalHistoryConverter;
import com.umc.puppymode2.domain.goal.dto.GoalPostRequestDTO;
import com.umc.puppymode2.domain.goal.entity.UserGoalHistory;
import com.umc.puppymode2.domain.goal.repository.UserGoalHistoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserGoalHistoryCommandServiceImplTest {

    @Mock
    private UserGoalHistoryRepository repository;

    @Mock
    private UserGoalHistoryConverter converter;

    @InjectMocks
    private UserGoalHistoryCommandServiceImpl service;

    private final Long userId = 1L;

    @Test
    void 새로운_목표_설정_성공() {

        GoalPostRequestDTO dto = new GoalPostRequestDTO(true, 5);

        UserGoalHistory entity = UserGoalHistory.builder()
                .userId(userId)
                .monthlyGoalCount(5)
                .goalMonth(LocalDate.of(2025,3,1))
                .goalSetAt(LocalDateTime.now())
                .build();

        when(repository.existsByUserIdAndGoalMonth(anyLong(), any()))
                .thenReturn(false);

        when(converter.toEntity(dto, userId))
                .thenReturn(entity);

        var result = service.postGoal(userId, dto);

        assertTrue(result.isSuccess());
        verify(repository).save(entity);
    }

    @Test
    void 목표_null이면_예외() {

        GoalPostRequestDTO dto = new GoalPostRequestDTO(true, null);

        assertThrows(IllegalArgumentException.class,
                () -> service.postGoal(userId, dto));
    }

    @Test
    void 목표값이_0이면_예외() {

        GoalPostRequestDTO dto = new GoalPostRequestDTO(true, 0);

        assertThrows(IllegalArgumentException.class,
                () -> service.postGoal(userId, dto));
    }

    @Test
    void 목표값_범위_초과() {

        GoalPostRequestDTO dto = new GoalPostRequestDTO(true, 50);

        assertThrows(IllegalArgumentException.class,
                () -> service.postGoal(userId, dto));
    }

    @Test
    void 이미_목표가_존재하면_예외() {

        GoalPostRequestDTO dto = new GoalPostRequestDTO(true, 5);

        when(repository.existsByUserIdAndGoalMonth(anyLong(), any()))
                .thenReturn(true);

        assertThrows(IllegalStateException.class,
                () -> service.postGoal(userId, dto));

        verify(repository, never()).save(any());
    }

    @Test
    void 기존_목표_유지_성공() {

        GoalPostRequestDTO dto = new GoalPostRequestDTO(false, null);

        UserGoalHistory lastGoal = UserGoalHistory.builder()
                .monthlyGoalCount(3)
                .goalMonth(LocalDate.of(2025,2,1))
                .goalSetAt(LocalDateTime.now())
                .build();

        when(repository.existsByUserIdAndGoalMonth(anyLong(), any()))
                .thenReturn(false);

        when(repository.findTopByUserIdOrderByGoalMonthDesc(userId))
                .thenReturn(Optional.of(lastGoal));

        var result = service.postGoal(userId, dto);

        assertTrue(result.isSuccess());
        verify(repository).save(any());
    }

    @Test
    void 기존_목표가_없으면_예외() {

        GoalPostRequestDTO dto = new GoalPostRequestDTO(false, null);

        when(repository.findTopByUserIdOrderByGoalMonthDesc(userId))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> service.postGoal(userId, dto));
    }

    @Test
    void 기존_목표가_이번달_최대일수를_초과하면_보정된다() {

        GoalPostRequestDTO dto = new GoalPostRequestDTO(false, null);

        UserGoalHistory lastGoal = UserGoalHistory.builder()
                .monthlyGoalCount(31)
                .goalMonth(LocalDate.of(2025,1,1))
                .goalSetAt(LocalDateTime.now())
                .build();

        when(repository.existsByUserIdAndGoalMonth(anyLong(), any()))
                .thenReturn(false);

        when(repository.findTopByUserIdOrderByGoalMonthDesc(userId))
                .thenReturn(Optional.of(lastGoal));

        service.postGoal(userId, dto);

        verify(repository).save(argThat(goal ->
                goal.getMonthlyGoalCount() ==
                        Math.min(31, LocalDate.now().lengthOfMonth())
        ));
    }
}