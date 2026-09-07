package com.umc.puppymode2.domain.report.service;

import com.umc.puppymode2.domain.advice.repository.AdviceRepository;
import com.umc.puppymode2.domain.drinkhistory.repository.DrinkHistoryRepository;
import com.umc.puppymode2.domain.goal.entity.UserGoalHistory;
import com.umc.puppymode2.domain.goal.repository.UserGoalHistoryRepository;
import com.umc.puppymode2.domain.report.converter.DrinkReportConverter;
import com.umc.puppymode2.domain.report.dto.DrinkReportResponseDTO;
import com.umc.puppymode2.domain.report.dto.GoalStatus;
import com.umc.puppymode2.global.cache.DrinkReportCacheService;
import com.umc.puppymode2.global.util.TimeConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class DrinkReportServiceImplTest {

    @Mock
    private UserGoalHistoryRepository userGoalHistoryRepository;

    @Mock
    private DrinkHistoryRepository drinkHistoryRepository;

    @Mock
    private AdviceRepository adviceRepository;

    @Mock
    private DrinkReportConverter drinkReportConverter;

    @Mock
    private DrinkReportCacheService reportCacheService;

    @InjectMocks
    private DrinkReportServiceImpl drinkReportService;

    private Long userId;
    private YearMonth targetMonth;

    @BeforeEach
    void setUp() {
        userId = 1L;
        targetMonth = YearMonth.of(2024, 3);
    }

    @Test
    void 월간_음주_리포트_정상조회() {

        // given
        int goal = 15;
        long drinkDays = 5L;
        long drinkRecordCount = 8L;
        int scoldedCount = 3;

        UserGoalHistory goalHistory = mock(UserGoalHistory.class);
        when(goalHistory.getMonthlyGoalCount()).thenReturn(goal);

        when(userGoalHistoryRepository
                .findByUserIdAndGoalMonth(
                        eq(userId),
                        eq(targetMonth.atDay(1))))
                .thenReturn(Optional.of(goalHistory));

        when(drinkHistoryRepository
                .countByUserUserIdAndIsDrinkTrueAndDrinkDateBetween(
                        eq(userId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(drinkDays);

        when(drinkHistoryRepository
                .countByUserUserIdAndDrinkDateBetween(
                        eq(userId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(drinkRecordCount);

        when(adviceRepository
                .countByUserUserIdAndAdvisedAtBetween(
                        eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn((long) scoldedCount);

        DrinkReportResponseDTO dto = mock(DrinkReportResponseDTO.class);

        when(drinkReportConverter.toDto(
                eq(goal),
                eq(drinkRecordCount),
                eq(drinkDays),
                anyInt(),
                eq(scoldedCount),
                any()
        )).thenReturn(dto);

        // when
        DrinkReportResponseDTO result =
                drinkReportService.drinkReport(userId, targetMonth);

        // then
        assertThat(result).isEqualTo(dto);

        verify(userGoalHistoryRepository)
                .findByUserIdAndGoalMonth(
                        userId,
                        targetMonth.atDay(1)
                );

        verify(drinkHistoryRepository, times(1))
                .countByUserUserIdAndIsDrinkTrueAndDrinkDateBetween(any(), any(), any());

        verify(drinkHistoryRepository, times(1))
                .countByUserUserIdAndDrinkDateBetween(any(), any(), any());

        verify(adviceRepository, times(1))
                .countByUserUserIdAndAdvisedAtBetween(any(), any(), any());

        verify(drinkReportConverter, times(1))
                .toDto(eq(goal), eq(drinkRecordCount), eq(drinkDays), anyInt(), eq(scoldedCount), any());
    }

    @Test
    void 목표가_없는_경우_기본값_15로_설정된다() {

        // given
        long drinkDays = 2L;
        long drinkRecordCount = 5L;
        int scoldedCount = 1;

        when(userGoalHistoryRepository
                .findByUserIdAndGoalMonth(
                        eq(userId),
                        eq(targetMonth.atDay(1))))
                .thenReturn(Optional.empty());

        when(drinkHistoryRepository
                .countByUserUserIdAndIsDrinkTrueAndDrinkDateBetween(
                        eq(userId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(drinkDays);

        when(drinkHistoryRepository
                .countByUserUserIdAndDrinkDateBetween(
                        eq(userId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(drinkRecordCount);

        when(adviceRepository
                .countByUserUserIdAndAdvisedAtBetween(
                        eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn((long) scoldedCount);

        DrinkReportResponseDTO dto = mock(DrinkReportResponseDTO.class);

        when(drinkReportConverter.toDto(
                eq(15), // 기본 목표값
                eq(drinkRecordCount),
                eq(drinkDays),
                anyInt(),
                eq(scoldedCount),
                any()
        )).thenReturn(dto);

        // when
        DrinkReportResponseDTO result =
                drinkReportService.drinkReport(userId, targetMonth);

        // then
        assertThat(result).isEqualTo(dto);

        verify(drinkReportConverter).toDto(
                eq(15),
                eq(drinkRecordCount),
                eq(drinkDays),
                anyInt(),
                eq(scoldedCount),
                any()
        );
    }

    @Test
    void 음주일이_목표보다_적으면_달성확률은_0에서_100사이이다() {
        // given
        int goal = 10;
        long drinkDays = 2L;
        long drinkRecordCount = 5L;
        int scoldedCount = 0;

        UserGoalHistory goalHistory = mock(UserGoalHistory.class);
        when(goalHistory.getMonthlyGoalCount()).thenReturn(goal);

        when(userGoalHistoryRepository
                .findByUserIdAndGoalMonth(
                        eq(userId),
                        eq(targetMonth.atDay(1))))
                .thenReturn(Optional.of(goalHistory));

        when(drinkHistoryRepository
                .countByUserUserIdAndIsDrinkTrueAndDrinkDateBetween(
                        eq(userId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(drinkDays);

        when(drinkHistoryRepository
                .countByUserUserIdAndDrinkDateBetween(
                        eq(userId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(drinkRecordCount);

        when(adviceRepository
                .countByUserUserIdAndAdvisedAtBetween(
                        eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn((long) scoldedCount);

        DrinkReportResponseDTO dto = mock(DrinkReportResponseDTO.class);

        when(drinkReportConverter.toDto(
                eq(goal),
                eq(drinkRecordCount),
                eq(drinkDays),
                anyInt(),
                eq(scoldedCount),
                any()
        )).thenReturn(dto);

        // when
        DrinkReportResponseDTO result =
                drinkReportService.drinkReport(userId, targetMonth);

        // then
        assertThat(result).isEqualTo(dto);

        ArgumentCaptor<Integer> achievementRateCaptor =
                ArgumentCaptor.forClass(Integer.class);

        verify(drinkReportConverter).toDto(
                eq(goal),
                eq(drinkRecordCount),
                eq(drinkDays),
                achievementRateCaptor.capture(),
                eq(scoldedCount),
                any()
        );

        assertThat(achievementRateCaptor.getValue())
                .isBetween(0, 100);
    }

    @Test
    void 음주일이_목표보다_많으면_달성확률은_0이다() {
        // given
        int goal = 5;
        long drinkDays = 8L;
        long drinkRecordCount = 10L;
        int scoldedCount = 0;

        UserGoalHistory goalHistory = mock(UserGoalHistory.class);
        when(goalHistory.getMonthlyGoalCount()).thenReturn(goal);

        when(userGoalHistoryRepository
                .findByUserIdAndGoalMonth(
                        eq(userId),
                        eq(targetMonth.atDay(1))))
                .thenReturn(Optional.of(goalHistory));

        when(drinkHistoryRepository
                .countByUserUserIdAndIsDrinkTrueAndDrinkDateBetween(
                        eq(userId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(drinkDays);

        when(drinkHistoryRepository
                .countByUserUserIdAndDrinkDateBetween(
                        eq(userId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(drinkRecordCount);

        when(adviceRepository
                .countByUserUserIdAndAdvisedAtBetween(
                        eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn((long) scoldedCount);

        DrinkReportResponseDTO dto = mock(DrinkReportResponseDTO.class);

        when(drinkReportConverter.toDto(
                eq(goal),
                eq(drinkRecordCount),
                eq(drinkDays),
                eq(0),
                eq(scoldedCount),
                any()
        )).thenReturn(dto);

        // when
        DrinkReportResponseDTO result =
                drinkReportService.drinkReport(userId, targetMonth);

        // then
        assertThat(result).isEqualTo(dto);

        verify(drinkReportConverter).toDto(
                eq(goal),
                eq(drinkRecordCount),
                eq(drinkDays),
                eq(0),
                eq(scoldedCount),
                any()
        );
    }

    @Test
    void 목표가_0이면_달성확률은_0이다() {
        // given
        int goal = 0;
        long drinkDays = 3L;
        long drinkRecordCount = 5L;
        int scoldedCount = 0;

        UserGoalHistory goalHistory = mock(UserGoalHistory.class);
        when(goalHistory.getMonthlyGoalCount()).thenReturn(goal);

        when(userGoalHistoryRepository
                .findByUserIdAndGoalMonth(
                        eq(userId),
                        eq(targetMonth.atDay(1))))
                .thenReturn(Optional.of(goalHistory));

        when(drinkHistoryRepository
                .countByUserUserIdAndIsDrinkTrueAndDrinkDateBetween(
                        eq(userId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(drinkDays);

        when(drinkHistoryRepository
                .countByUserUserIdAndDrinkDateBetween(
                        eq(userId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(drinkRecordCount);

        when(adviceRepository
                .countByUserUserIdAndAdvisedAtBetween(
                        eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn((long) scoldedCount);

        DrinkReportResponseDTO dto = mock(DrinkReportResponseDTO.class);

        when(drinkReportConverter.toDto(
                eq(goal),
                eq(drinkRecordCount),
                eq(drinkDays),
                eq(0),
                eq(scoldedCount),
                any()
        )).thenReturn(dto);

        // when
        DrinkReportResponseDTO result =
                drinkReportService.drinkReport(userId, targetMonth);

        // then
        assertThat(result).isEqualTo(dto);

        verify(drinkReportConverter).toDto(
                eq(goal),
                eq(drinkRecordCount),
                eq(drinkDays),
                eq(0),
                eq(scoldedCount),
                any()
        );
    }

    /**
     * #168과 동일한 원인의 회귀 테스트.
     *
     * resolveCurrentDay()가 LocalDate.now()를 타임존 없이 호출하면, 서버 JVM 기본 타임존이
     * UTC일 때 한국 시간 00:00~08:59 사이에 날짜가 하루 어긋난다. 실제 벽시계 시각에
     * 의존하면 이 버그는 하루 중 특정 시간대에만 재현되는 flaky한 테스트가 되므로,
     * LocalDate.now(TimeConstants.KST) 호출 자체를 static mock으로 가로채 결정론적으로 검증한다.
     */
    @Test
    void 이번달_리포트는_LocalDate_now_KST로_오늘_날짜를_계산한다() {
        // given
        YearMonth currentMonth = YearMonth.of(2025, 8);
        LocalDate fixedToday = LocalDate.of(2025, 8, 20);

        when(userGoalHistoryRepository
                .findByUserIdAndGoalMonth(eq(userId), eq(currentMonth.atDay(1))))
                .thenReturn(Optional.empty());
        when(drinkHistoryRepository
                .countByUserUserIdAndIsDrinkTrueAndDrinkDateBetween(eq(userId), any(), any()))
                .thenReturn(0L);
        when(drinkHistoryRepository
                .countByUserUserIdAndDrinkDateBetween(eq(userId), any(), any()))
                .thenReturn(0L);
        when(adviceRepository
                .countByUserUserIdAndAdvisedAtBetween(eq(userId), any(), any()))
                .thenReturn(0L);
        when(drinkReportConverter.toDto(anyInt(), anyLong(), anyLong(), anyInt(), anyInt(), any()))
                .thenReturn(mock(DrinkReportResponseDTO.class));

        // when & then
        try (MockedStatic<LocalDate> mockedLocalDate = mockStatic(LocalDate.class, CALLS_REAL_METHODS)) {
            mockedLocalDate.when(() -> LocalDate.now(TimeConstants.KST)).thenReturn(fixedToday);

            drinkReportService.drinkReport(userId, currentMonth);

            // KST를 명시한 오버로드가 실제로 호출됐는지
            mockedLocalDate.verify(() -> LocalDate.now(TimeConstants.KST), atLeastOnce());
            // 타임존 없는 오버로드(버그의 원인)는 절대 호출되면 안 됨
            mockedLocalDate.verify(LocalDate::now, never());
        }
    }

    // ---------------------------------------------------------------------
    // goalStatus 판정 (#184)
    // ---------------------------------------------------------------------

    @Test
    void 목표가_없으면_goalStatus는_NO_GOAL이다() {
        // targetMonth(2024-03)는 항상 과거이지만, 목표 자체가 없으므로 과거/현재 판정 이전에 NO_GOAL
        stubDrinkReportBasics(Optional.empty(), 0L, 0L);

        drinkReportService.drinkReport(userId, targetMonth);

        assertThat(captureGoalStatus()).isEqualTo(GoalStatus.NO_GOAL);
    }

    @Test
    void 과거월에_실제_음주일수가_목표_이하이고_기록이_있으면_ACHIEVED이다() {
        UserGoalHistory goalHistory = mock(UserGoalHistory.class);
        when(goalHistory.getMonthlyGoalCount()).thenReturn(10);

        // drinkDays(3) <= goal(10), drinkRecordCount(5) > 0
        stubDrinkReportBasics(Optional.of(goalHistory), 3L, 5L);

        drinkReportService.drinkReport(userId, targetMonth);

        assertThat(captureGoalStatus()).isEqualTo(GoalStatus.ACHIEVED);
    }

    @Test
    void 과거월에_실제_음주일수가_목표를_초과하면_FAILED이다() {
        UserGoalHistory goalHistory = mock(UserGoalHistory.class);
        when(goalHistory.getMonthlyGoalCount()).thenReturn(5);

        // drinkDays(8) > goal(5)
        stubDrinkReportBasics(Optional.of(goalHistory), 8L, 10L);

        drinkReportService.drinkReport(userId, targetMonth);

        assertThat(captureGoalStatus()).isEqualTo(GoalStatus.FAILED);
    }

    @Test
    void 과거월에_목표는_있지만_음주_기록이_0건이면_FAILED이다() {
        UserGoalHistory goalHistory = mock(UserGoalHistory.class);
        when(goalHistory.getMonthlyGoalCount()).thenReturn(5);

        // 목표만 세우고 그 달에 기록을 한 번도 남기지 않은 경우 → 달성으로 보지 않는다
        stubDrinkReportBasics(Optional.of(goalHistory), 0L, 0L);

        drinkReportService.drinkReport(userId, targetMonth);

        assertThat(captureGoalStatus()).isEqualTo(GoalStatus.FAILED);
    }

    @Test
    void 조회월이_이번_달이면_goalStatus는_IN_PROGRESS이다() {
        YearMonth currentMonth = YearMonth.of(2025, 8);
        LocalDate fixedToday = LocalDate.of(2025, 8, 20);

        UserGoalHistory goalHistory = mock(UserGoalHistory.class);
        when(goalHistory.getMonthlyGoalCount()).thenReturn(10);

        when(userGoalHistoryRepository
                .findByUserIdAndGoalMonth(eq(userId), eq(currentMonth.atDay(1))))
                .thenReturn(Optional.of(goalHistory));
        when(drinkHistoryRepository
                .countByUserUserIdAndIsDrinkTrueAndDrinkDateBetween(eq(userId), any(), any()))
                .thenReturn(2L);
        when(drinkHistoryRepository
                .countByUserUserIdAndDrinkDateBetween(eq(userId), any(), any()))
                .thenReturn(4L);
        when(adviceRepository
                .countByUserUserIdAndAdvisedAtBetween(eq(userId), any(), any()))
                .thenReturn(0L);
        when(drinkReportConverter.toDto(anyInt(), anyLong(), anyLong(), anyInt(), anyInt(), any()))
                .thenReturn(mock(DrinkReportResponseDTO.class));

        try (MockedStatic<LocalDate> mockedLocalDate = mockStatic(LocalDate.class, CALLS_REAL_METHODS)) {
            mockedLocalDate.when(() -> LocalDate.now(TimeConstants.KST)).thenReturn(fixedToday);

            drinkReportService.drinkReport(userId, currentMonth);
        }

        assertThat(captureGoalStatus()).isEqualTo(GoalStatus.IN_PROGRESS);
    }

    /**
     * drinkReport()가 캐시 미스 경로를 타도록 최소한의 스텁만 세팅한다.
     *
     * @param goalOpt          targetMonth 목표 조회 결과
     * @param drinkDays        isDrink=true 인 날 수
     * @param drinkRecordCount 해당 월 전체 기록 수(true+false)
     */
    private void stubDrinkReportBasics(Optional<UserGoalHistory> goalOpt, long drinkDays, long drinkRecordCount) {
        when(userGoalHistoryRepository
                .findByUserIdAndGoalMonth(eq(userId), eq(targetMonth.atDay(1))))
                .thenReturn(goalOpt);
        when(drinkHistoryRepository
                .countByUserUserIdAndIsDrinkTrueAndDrinkDateBetween(eq(userId), any(), any()))
                .thenReturn(drinkDays);
        when(drinkHistoryRepository
                .countByUserUserIdAndDrinkDateBetween(eq(userId), any(), any()))
                .thenReturn(drinkRecordCount);
        when(adviceRepository
                .countByUserUserIdAndAdvisedAtBetween(eq(userId), any(), any()))
                .thenReturn(0L);
        when(drinkReportConverter.toDto(anyInt(), anyLong(), anyLong(), anyInt(), anyInt(), any()))
                .thenReturn(mock(DrinkReportResponseDTO.class));
    }

    /** drinkReportConverter.toDto(...)에 전달된 goalStatus 인자를 캡처한다. */
    private GoalStatus captureGoalStatus() {
        ArgumentCaptor<GoalStatus> captor = ArgumentCaptor.forClass(GoalStatus.class);
        verify(drinkReportConverter).toDto(
                anyInt(), anyLong(), anyLong(), anyInt(), anyInt(), captor.capture());
        return captor.getValue();
    }

}