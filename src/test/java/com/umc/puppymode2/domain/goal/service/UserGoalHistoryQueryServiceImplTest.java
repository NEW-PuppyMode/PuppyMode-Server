package com.umc.puppymode2.domain.goal.service;

import com.umc.puppymode2.domain.drinkhistory.repository.DrinkHistoryRepository;
import com.umc.puppymode2.domain.goal.converter.UserGoalHistoryConverter;
import com.umc.puppymode2.domain.goal.dto.GoalInfoResponseDTO;
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
class UserGoalHistoryQueryServiceImplTest {

    @Mock
    private UserGoalHistoryRepository repository;

    @Mock
    private UserGoalHistoryConverter converter;

    @Mock
    private DrinkHistoryRepository drinkHistoryRepository;

    @InjectMocks
    private UserGoalHistoryQueryServiceImpl service;

    private final Long userId = 1L;

    @Test
    void 목표_조회_성공() {

        LocalDate goalMonth = LocalDate.now().withDayOfMonth(1);

        UserGoalHistory goal = UserGoalHistory.builder()
                .userId(userId)
                .goalMonth(goalMonth)
                .monthlyGoalCount(5)
                .goalSetAt(LocalDateTime.now())
                .build();

        GoalInfoResponseDTO dto = GoalInfoResponseDTO.builder()
                .monthlyGoalCount(5)
                .monthlyActualCount(2L)
                .isGoalExceeded(false)
                .build();

        when(repository.findByUserIdAndGoalMonth(userId, goalMonth))
                .thenReturn(Optional.of(goal));

        when(drinkHistoryRepository
                .countByUserUserIdAndIsDrinkTrueAndDrinkDateBetween(anyLong(), any(), any()))
                .thenReturn(2L);

        when(converter.toDto(goal, 2L))
                .thenReturn(dto);

        var result = service.getLatestGoal(userId);

        assertNotNull(result);
        assertEquals(5, result.getMonthlyGoalCount());
        assertEquals(2, result.getMonthlyActualCount());
    }

    @Test
    void 목표가_없으면_null() {

        LocalDate goalMonth = LocalDate.now().withDayOfMonth(1);

        when(repository.findByUserIdAndGoalMonth(userId, goalMonth))
                .thenReturn(Optional.empty());

        var result = service.getLatestGoal(userId);

        assertNull(result);
    }

    @Test
    void 이번달_목표_없으면_30일_지남_true() {

        LocalDate goalMonth = LocalDate.now().withDayOfMonth(1);

        when(repository.findByUserIdAndGoalMonth(userId, goalMonth))
                .thenReturn(Optional.empty());

        assertTrue(service.isMoreThan30DayPassed(userId));
    }

    @Test
    void 이번달_목표_있으면_false() {

        LocalDate goalMonth = LocalDate.now().withDayOfMonth(1);

        UserGoalHistory goal = UserGoalHistory.builder()
                .goalMonth(goalMonth)
                .build();

        when(repository.findByUserIdAndGoalMonth(userId, goalMonth))
                .thenReturn(Optional.of(goal));

        assertFalse(service.isMoreThan30DayPassed(userId));
    }

    @Test
    void 목표보다_실제_음주가_많으면_goalExceeded_true() {

        LocalDate goalMonth = LocalDate.of(2025,3,1);

        UserGoalHistory goal = UserGoalHistory.builder()
                .userId(userId)
                .goalMonth(goalMonth)
                .monthlyGoalCount(5)
                .build();

        when(repository.findByUserIdAndGoalMonth(userId, goalMonth))
                .thenReturn(Optional.of(goal));

        when(drinkHistoryRepository
                .countByUserUserIdAndIsDrinkTrueAndDrinkDateBetween(any(), any(), any()))
                .thenReturn(6L);

        when(converter.toDto(goal, 6L))
                .thenCallRealMethod();

        var result = service.getLatestGoal(userId);

        assertTrue(result.getIsGoalExceeded());
    }

    @Test
    void 목표와_실제_음주가_같으면_goalExceeded_true() {

        LocalDate goalMonth = LocalDate.now().withDayOfMonth(1);

        UserGoalHistory goal = UserGoalHistory.builder()
                .userId(userId)
                .goalMonth(goalMonth)
                .monthlyGoalCount(5)
                .build();

        when(repository.findByUserIdAndGoalMonth(userId, goalMonth))
                .thenReturn(Optional.of(goal));

        when(drinkHistoryRepository
                .countByUserUserIdAndIsDrinkTrueAndDrinkDateBetween(any(), any(), any()))
                .thenReturn(5L);

        when(converter.toDto(goal, 5L))
                .thenCallRealMethod();

        var result = service.getLatestGoal(userId);

        assertTrue(result.getIsGoalExceeded());
    }
}