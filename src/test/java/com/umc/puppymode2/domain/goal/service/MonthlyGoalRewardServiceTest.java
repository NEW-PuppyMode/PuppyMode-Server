package com.umc.puppymode2.domain.goal.service;

import com.umc.puppymode2.domain.drinkhistory.repository.DrinkHistoryRepository;
import com.umc.puppymode2.domain.drinkhistory.repository.UserDrinkCountProjection;
import com.umc.puppymode2.domain.goal.dto.MonthlyGoalRewardResponseDTO;
import com.umc.puppymode2.domain.goal.entity.UserGoalHistory;
import com.umc.puppymode2.domain.goal.repository.UserGoalHistoryRepository;
import com.umc.puppymode2.domain.puppy.entity.Puppy;
import com.umc.puppymode2.domain.puppy.repository.PuppyRepository;
import com.umc.puppymode2.domain.puppy.service.PuppyLevelCalculator;
import com.umc.puppymode2.global.cache.DrinkReportCacheService;
import com.umc.puppymode2.global.exception.GeneralException;
import com.umc.puppymode2.global.util.TimeConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MonthlyGoalRewardServiceTest {

    @Mock
    private UserGoalHistoryRepository userGoalHistoryRepository;

    @Mock
    private DrinkHistoryRepository drinkHistoryRepository;

    @Mock
    private PuppyRepository puppyRepository;

    @Mock
    private PuppyLevelCalculator puppyLevelCalculator;

    @Mock
    private DrinkReportCacheService reportCacheService;

    @InjectMocks
    private MonthlyGoalRewardService monthlyGoalRewardService;

    private final Long userId = 1L;

    // 항상 "지난 달"이 되도록 고정된 과거 월 사용
    private final YearMonth pastMonth = YearMonth.of(2025, 9);

    @Test
    @DisplayName("20일 이상 기록하고 음주 횟수가 목표 이하면 경험치 100을 지급하고 before/after를 반환한다")
    void 조건_충족시_경험치_100_지급() {
        // given
        UserGoalHistory goal = mock(UserGoalHistory.class);
        when(goal.isRewarded()).thenReturn(false);
        when(goal.getMonthlyGoalCount()).thenReturn(10);
        when(userGoalHistoryRepository.findForUpdate(userId, pastMonth.atDay(1)))
                .thenReturn(Optional.of(goal));

        stubStat(20L, 5L);

        Puppy puppy = mock(Puppy.class);
        when(puppy.getPuppyExp()).thenReturn(200);
        when(puppyRepository.findByUser_UserId(userId)).thenReturn(Optional.of(puppy));

        when(puppyLevelCalculator.calculate(200)).thenReturn(new PuppyLevelCalculator.LevelProgress(8, 55));
        when(puppyLevelCalculator.calculate(300)).thenReturn(new PuppyLevelCalculator.LevelProgress(10, 54));

        // when
        MonthlyGoalRewardResponseDTO result = monthlyGoalRewardService.grantReward(userId, pastMonth);

        // then
        verify(puppy).setPuppyExp(300);
        verify(goal).markRewarded(200, 300);
        verify(reportCacheService).evict(userId, pastMonth);

        assertThat(result.isRewarded()).isTrue();
        assertThat(result.getGoalReward().getBefore().getLevel()).isEqualTo(8);
        assertThat(result.getGoalReward().getBefore().getPercent()).isEqualTo(55);
        assertThat(result.getGoalReward().getAfter().getLevel()).isEqualTo(10);
        assertThat(result.getGoalReward().getAfter().getPercent()).isEqualTo(54);
    }

    @Test
    @DisplayName("이미 보상을 받은 달이면 다시 지급하지 않고 저장된 before/after를 반환한다")
    void 이미_받은_달이면_재지급하지_않는다() {
        // given
        UserGoalHistory goal = mock(UserGoalHistory.class);
        when(goal.isRewarded()).thenReturn(true);
        when(goal.getRewardExpBefore()).thenReturn(200);
        when(goal.getRewardExpAfter()).thenReturn(300);
        when(userGoalHistoryRepository.findForUpdate(userId, pastMonth.atDay(1)))
                .thenReturn(Optional.of(goal));

        when(puppyLevelCalculator.calculate(200)).thenReturn(new PuppyLevelCalculator.LevelProgress(8, 55));
        when(puppyLevelCalculator.calculate(300)).thenReturn(new PuppyLevelCalculator.LevelProgress(10, 54));

        // when
        MonthlyGoalRewardResponseDTO result = monthlyGoalRewardService.grantReward(userId, pastMonth);

        // then
        assertThat(result.isRewarded()).isTrue();
        assertThat(result.getGoalReward().getAfter().getLevel()).isEqualTo(10);

        verify(goal, never()).markRewarded(anyInt(), anyInt());
        verify(puppyRepository, never()).findByUser_UserId(any());
        verify(drinkHistoryRepository, never()).countMonthlyDrinkByUserIds(any(), any(), any());
        verify(reportCacheService, never()).evict(any(), any());
    }

    @Test
    @DisplayName("기록이 19일이면 목표를 달성해도 지급하지 않는다")
    void 기록_19일이면_지급하지_않는다() {
        // given
        UserGoalHistory goal = mock(UserGoalHistory.class);
        when(goal.isRewarded()).thenReturn(false);
        when(userGoalHistoryRepository.findForUpdate(userId, pastMonth.atDay(1)))
                .thenReturn(Optional.of(goal));

        stubStat(19L, 0L);

        // when
        MonthlyGoalRewardResponseDTO result = monthlyGoalRewardService.grantReward(userId, pastMonth);

        // then
        assertThat(result.isRewarded()).isFalse();
        assertThat(result.getGoalReward()).isNull();
        verify(goal, never()).markRewarded(anyInt(), anyInt());
        verify(reportCacheService, never()).evict(any(), any());
    }

    @Test
    @DisplayName("음주 횟수가 목표를 초과하면 지급하지 않는다")
    void 목표_초과면_지급하지_않는다() {
        // given
        UserGoalHistory goal = mock(UserGoalHistory.class);
        when(goal.isRewarded()).thenReturn(false);
        when(goal.getMonthlyGoalCount()).thenReturn(3);
        when(userGoalHistoryRepository.findForUpdate(userId, pastMonth.atDay(1)))
                .thenReturn(Optional.of(goal));

        stubStat(25L, 4L);

        // when
        MonthlyGoalRewardResponseDTO result = monthlyGoalRewardService.grantReward(userId, pastMonth);

        // then
        assertThat(result.isRewarded()).isFalse();
        assertThat(result.getGoalReward()).isNull();
        verify(goal, never()).markRewarded(anyInt(), anyInt());
        verify(reportCacheService, never()).evict(any(), any());
    }

    @Test
    @DisplayName("그 달 목표가 없으면 지급하지 않는다")
    void 목표가_없으면_지급하지_않는다() {
        // given
        when(userGoalHistoryRepository.findForUpdate(userId, pastMonth.atDay(1)))
                .thenReturn(Optional.empty());

        // when
        MonthlyGoalRewardResponseDTO result = monthlyGoalRewardService.grantReward(userId, pastMonth);

        // then
        assertThat(result.isRewarded()).isFalse();
        assertThat(result.getGoalReward()).isNull();
        verify(drinkHistoryRepository, never()).countMonthlyDrinkByUserIds(any(), any(), any());
        verify(reportCacheService, never()).evict(any(), any());
    }

    @Test
    @DisplayName("이번 달(진행 중) 보상을 요청하면 예외가 발생한다")
    void 이번_달_요청은_예외() {
        // given
        YearMonth currentMonth = YearMonth.now(TimeConstants.KST);

        // when & then
        assertThatThrownBy(() -> monthlyGoalRewardService.grantReward(userId, currentMonth))
                .isInstanceOf(GeneralException.class);

        verify(userGoalHistoryRepository, never()).findForUpdate(any(), any());
    }

    private void stubStat(long recordDays, long drinkCount) {
        UserDrinkCountProjection stat = mock(UserDrinkCountProjection.class);
        when(stat.getRecordDays()).thenReturn(recordDays);
        lenient().when(stat.getDrinkCount()).thenReturn(drinkCount);
        when(drinkHistoryRepository.countMonthlyDrinkByUserIds(eq(List.of(userId)), any(), any()))
                .thenReturn(List.of(stat));
    }
}
