package com.umc.puppymode2.domain.goal.service;

import com.umc.puppymode2.domain.drinkhistory.repository.DrinkHistoryRepository;
import com.umc.puppymode2.domain.goal.converter.UserGoalHistoryConverter;
import com.umc.puppymode2.domain.goal.dto.GoalInfoResponseDTO;
import com.umc.puppymode2.domain.goal.entity.UserGoalHistory;
import com.umc.puppymode2.domain.goal.repository.UserGoalHistoryRepository;
import com.umc.puppymode2.global.util.TimeConstants;
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

        LocalDate goalMonth = LocalDate.now(TimeConstants.KST).withDayOfMonth(1);

        UserGoalHistory goal = UserGoalHistory.builder()
                .userId(userId)
                .goalMonth(goalMonth)
                .monthlyGoalCount(5)
                .goalSetAt(LocalDateTime.now(TimeConstants.KST))
                .build();

        GoalInfoResponseDTO dto = GoalInfoResponseDTO.builder()
                .monthlyGoalCount(5)
                .monthlyActualCount(2L)
                .isGoalExceeded(false)
                .build();

        when(repository.findByUserIdAndGoalMonth(userId, goalMonth))
                .thenReturn(Optional.of(goal));
        when(repository.findTopByUserIdOrderByGoalMonthDesc(userId))
                .thenReturn(Optional.of(goal));

        when(drinkHistoryRepository
                .countByUserUserIdAndIsDrinkTrueAndDrinkDateBetween(anyLong(), any(), any()))
                .thenReturn(2L);

        when(converter.toDto(goal, 2L, goal))
                .thenReturn(dto);

        var result = service.getLatestGoal(userId);

        assertNotNull(result);
        assertEquals(5, result.getMonthlyGoalCount());
        assertEquals(2, result.getMonthlyActualCount());
    }

    @Test
    void 이번달_목표가_없어도_최근_목표_연월이_담긴_DTO를_반환한다() {

        LocalDate goalMonth = LocalDate.now(TimeConstants.KST).withDayOfMonth(1);

        // 과거(2026-08)에 세운 목표만 있고 이번 달 목표는 없는 상황
        UserGoalHistory pastGoal = UserGoalHistory.builder()
                .userId(userId)
                .goalMonth(LocalDate.of(2026, 8, 1))
                .monthlyGoalCount(10)
                .goalSetAt(LocalDateTime.of(2026, 8, 1, 9, 0))
                .build();

        when(repository.findByUserIdAndGoalMonth(userId, goalMonth))
                .thenReturn(Optional.empty());
        when(repository.findTopByUserIdOrderByGoalMonthDesc(userId))
                .thenReturn(Optional.of(pastGoal));
        when(converter.toDto(null, null, pastGoal))
                .thenCallRealMethod();

        var result = service.getLatestGoal(userId);

        assertNotNull(result);
        assertNull(result.getMonthlyGoalCount());
        assertNull(result.getGoalSetAt());
        assertEquals(2026, result.getLatestGoalYear());
        assertEquals(8, result.getLatestGoalMonth());
        // 이번 달 목표가 없으면 음주 횟수 count 쿼리는 아예 호출하지 않는다
        verify(drinkHistoryRepository, never())
                .countByUserUserIdAndIsDrinkTrueAndDrinkDateBetween(any(), any(), any());
    }

    @Test
    void 목표_이력이_전혀_없으면_모든_필드가_null인_DTO를_반환한다() {

        LocalDate goalMonth = LocalDate.now(TimeConstants.KST).withDayOfMonth(1);

        when(repository.findByUserIdAndGoalMonth(userId, goalMonth))
                .thenReturn(Optional.empty());
        when(repository.findTopByUserIdOrderByGoalMonthDesc(userId))
                .thenReturn(Optional.empty());
        when(converter.toDto(null, null, null))
                .thenCallRealMethod();

        var result = service.getLatestGoal(userId);

        assertNotNull(result);
        assertNull(result.getMonthlyGoalCount());
        assertNull(result.getLatestGoalYear());
        assertNull(result.getLatestGoalMonth());
    }

    @Test
    void 이번달_목표_없으면_30일_지남_true() {

        LocalDate goalMonth = LocalDate.now(TimeConstants.KST).withDayOfMonth(1);

        when(repository.findByUserIdAndGoalMonth(userId, goalMonth))
                .thenReturn(Optional.empty());

        assertTrue(service.isMoreThan30DayPassed(userId));
    }

    @Test
    void 이번달_목표_있으면_false() {

        LocalDate goalMonth = LocalDate.now(TimeConstants.KST).withDayOfMonth(1);

        UserGoalHistory goal = UserGoalHistory.builder()
                .goalMonth(goalMonth)
                .build();

        when(repository.findByUserIdAndGoalMonth(userId, goalMonth))
                .thenReturn(Optional.of(goal));

        assertFalse(service.isMoreThan30DayPassed(userId));
    }

    @Test
    void 목표보다_실제_음주가_많으면_goalExceeded_true() {

        LocalDate goalMonth = LocalDate.now(TimeConstants.KST).withDayOfMonth(1);

        UserGoalHistory goal = UserGoalHistory.builder()
                .userId(userId)
                .goalMonth(goalMonth)
                .monthlyGoalCount(5)
                .build();

        when(repository.findByUserIdAndGoalMonth(userId, goalMonth))
                .thenReturn(Optional.of(goal));
        when(repository.findTopByUserIdOrderByGoalMonthDesc(userId))
                .thenReturn(Optional.of(goal));

        when(drinkHistoryRepository
                .countByUserUserIdAndIsDrinkTrueAndDrinkDateBetween(any(), any(), any()))
                .thenReturn(6L);

        when(converter.toDto(goal, 6L, goal))
                .thenCallRealMethod();

        var result = service.getLatestGoal(userId);

        assertTrue(result.getIsGoalExceeded());
    }

    @Test
    void 목표와_실제_음주가_같으면_goalExceeded_true() {

        LocalDate goalMonth = LocalDate.now(TimeConstants.KST).withDayOfMonth(1);

        UserGoalHistory goal = UserGoalHistory.builder()
                .userId(userId)
                .goalMonth(goalMonth)
                .monthlyGoalCount(5)
                .build();

        when(repository.findByUserIdAndGoalMonth(userId, goalMonth))
                .thenReturn(Optional.of(goal));
        when(repository.findTopByUserIdOrderByGoalMonthDesc(userId))
                .thenReturn(Optional.of(goal));

        when(drinkHistoryRepository
                .countByUserUserIdAndIsDrinkTrueAndDrinkDateBetween(any(), any(), any()))
                .thenReturn(5L);

        when(converter.toDto(goal, 5L, goal))
                .thenCallRealMethod();

        var result = service.getLatestGoal(userId);

        assertTrue(result.getIsGoalExceeded());
    }
}