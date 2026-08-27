package com.umc.puppymode2.domain.report.service;

import com.umc.puppymode2.domain.advice.repository.AdviceRepository;
import com.umc.puppymode2.domain.drinkhistory.repository.DrinkHistoryRepository;
import com.umc.puppymode2.domain.goal.entity.UserGoalHistory;
import com.umc.puppymode2.domain.goal.repository.UserGoalHistoryRepository;
import com.umc.puppymode2.domain.report.converter.DrinkReportConverter;
import com.umc.puppymode2.domain.report.dto.DrinkReportResponseDTO;
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
                eq(scoldedCount)
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
                .toDto(eq(goal), eq(drinkRecordCount), eq(drinkDays), anyInt(), eq(scoldedCount));
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
                eq(scoldedCount)
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
                eq(scoldedCount)
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
                eq(scoldedCount)
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
                eq(scoldedCount)
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
                eq(scoldedCount)
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
                eq(scoldedCount)
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
                eq(scoldedCount)
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
                eq(scoldedCount)
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
        when(drinkReportConverter.toDto(anyInt(), anyLong(), anyLong(), anyInt(), anyInt()))
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

}