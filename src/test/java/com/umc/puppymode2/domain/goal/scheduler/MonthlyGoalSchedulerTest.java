package com.umc.puppymode2.domain.goal.scheduler;

import com.umc.puppymode2.domain.drinkhistory.repository.DrinkHistoryRepository;
import com.umc.puppymode2.domain.drinkhistory.repository.UserDrinkCountProjection;
import com.umc.puppymode2.domain.goal.entity.UserGoalHistory;
import com.umc.puppymode2.domain.goal.repository.UserGoalHistoryRepository;
import com.umc.puppymode2.domain.puppy.entity.Puppy;
import com.umc.puppymode2.domain.puppy.repository.PuppyRepository;
import com.umc.puppymode2.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MonthlyGoalSchedulerTest {

    @Mock
    private UserGoalHistoryRepository userGoalHistoryRepository;

    @Mock
    private DrinkHistoryRepository drinkHistoryRepository;

    @Mock
    private PuppyRepository puppyRepository;

    @InjectMocks
    private MonthlyGoalScheduler monthlyGoalScheduler;

    @Test
    @DisplayName("이번 달 목표가 없으면 아무 작업도 수행하지 않는다")
    void 이번달_목표가_없으면_아무것도_실행되지_않는다() {
        // given
        when(userGoalHistoryRepository.findAllByGoalMonthAndRewardedFalse(any()))
                .thenReturn(List.of());

        // when
        monthlyGoalScheduler.evaluateMonthlyGoals();

        // then
        verify(puppyRepository, never()).findAllByUserUserIdIn(anyList());
        verify(drinkHistoryRepository, never()).countMonthlyDrinkByUserIds(anyList(), any(), any());
    }

    @Test
    @DisplayName("음주 횟수가 목표보다 적으면 경험치 300을 지급한다")
    void 음주_횟수가_목표_이하면_경험치_300이_지급된다() {
        // given
        Long userId = 1L;

        UserGoalHistory goal = mock(UserGoalHistory.class);
        when(goal.getUserId()).thenReturn(userId);
        when(goal.getMonthlyGoalCount()).thenReturn(3);

        Puppy puppy = mock(Puppy.class);
        User user = mock(User.class);
        when(user.getUserId()).thenReturn(userId);
        when(puppy.getUser()).thenReturn(user);
        when(puppy.getPuppyExp()).thenReturn(100);

        UserDrinkCountProjection stat = mock(UserDrinkCountProjection.class);
        when(stat.getUserId()).thenReturn(userId);
        when(stat.getTotalCount()).thenReturn(5L);
        when(stat.getDrinkCount()).thenReturn(2L);

        when(userGoalHistoryRepository.findAllByGoalMonthAndRewardedFalse(any()))
                .thenReturn(List.of(goal));
        when(puppyRepository.findAllByUserUserIdIn(anyList()))
                .thenReturn(List.of(puppy));
        when(drinkHistoryRepository.countMonthlyDrinkByUserIds(anyList(), any(), any()))
                .thenReturn(List.of(stat));

        // when
        monthlyGoalScheduler.evaluateMonthlyGoals();

        // then
        verify(puppy).setPuppyExp(400);
        verify(goal).markRewarded();
    }

    @Test
    @DisplayName("음주 횟수가 목표를 초과하면 경험치를 지급하지 않는다")
    void 음주_횟수가_목표_초과면_경험치가_지급되지_않는다() {
        // given
        Long userId = 1L;

        UserGoalHistory goal = mock(UserGoalHistory.class);
        when(goal.getUserId()).thenReturn(userId);
        when(goal.getMonthlyGoalCount()).thenReturn(3);

        Puppy puppy = mock(Puppy.class);
        User user = mock(User.class);
        when(user.getUserId()).thenReturn(userId);
        when(puppy.getUser()).thenReturn(user);

        UserDrinkCountProjection stat = mock(UserDrinkCountProjection.class);
        when(stat.getUserId()).thenReturn(userId);
        when(stat.getTotalCount()).thenReturn(5L);
        when(stat.getDrinkCount()).thenReturn(5L);

        when(userGoalHistoryRepository.findAllByGoalMonthAndRewardedFalse(any()))
                .thenReturn(List.of(goal));
        when(puppyRepository.findAllByUserUserIdIn(anyList()))
                .thenReturn(List.of(puppy));
        when(drinkHistoryRepository.countMonthlyDrinkByUserIds(anyList(), any(), any()))
                .thenReturn(List.of(stat));

        // when
        monthlyGoalScheduler.evaluateMonthlyGoals();

        // then
        verify(puppy, never()).setPuppyExp(anyInt());
        verify(goal, never()).markRewarded();
    }

    @Test
    @DisplayName("월간 기록이 아예 없는 유저는 경험치를 지급하지 않는다")
    void 기록을_남기지_않은_유저는_경험치가_지급되지_않는다() {
        Long userId = 1L;

        UserGoalHistory goal = mock(UserGoalHistory.class);
        when(goal.getUserId()).thenReturn(userId);

        Puppy puppy = mock(Puppy.class);
        User user = mock(User.class);
        when(user.getUserId()).thenReturn(userId);
        when(puppy.getUser()).thenReturn(user);

        when(userGoalHistoryRepository.findAllByGoalMonthAndRewardedFalse(any()))
                .thenReturn(List.of(goal));
        when(puppyRepository.findAllByUserUserIdIn(anyList()))
                .thenReturn(List.of(puppy));
        when(drinkHistoryRepository.countMonthlyDrinkByUserIds(anyList(), any(), any()))
                .thenReturn(List.of());

        monthlyGoalScheduler.evaluateMonthlyGoals();

        verify(puppy, never()).setPuppyExp(anyInt());
        verify(goal, never()).markRewarded();
    }

    @Test
    @DisplayName("음주 횟수가 목표와 같으면 경험치 300을 지급한다")
    void 음주_횟수가_목표와_같으면_경험치_300이_지급된다() {
        // given
        Long userId = 1L;

        UserGoalHistory goal = mock(UserGoalHistory.class);
        when(goal.getUserId()).thenReturn(userId);
        when(goal.getMonthlyGoalCount()).thenReturn(3);

        Puppy puppy = mock(Puppy.class);
        User user = mock(User.class);
        when(user.getUserId()).thenReturn(userId);
        when(puppy.getUser()).thenReturn(user);
        when(puppy.getPuppyExp()).thenReturn(100);

        UserDrinkCountProjection stat = mock(UserDrinkCountProjection.class);
        when(stat.getUserId()).thenReturn(userId);
        when(stat.getTotalCount()).thenReturn(3L);
        when(stat.getDrinkCount()).thenReturn(3L);

        when(userGoalHistoryRepository.findAllByGoalMonthAndRewardedFalse(any()))
                .thenReturn(List.of(goal));
        when(puppyRepository.findAllByUserUserIdIn(anyList()))
                .thenReturn(List.of(puppy));
        when(drinkHistoryRepository.countMonthlyDrinkByUserIds(anyList(), any(), any()))
                .thenReturn(List.of(stat));

        // when
        monthlyGoalScheduler.evaluateMonthlyGoals();

        // then
        verify(puppy).setPuppyExp(400);
        verify(goal).markRewarded();
    }

    @Test
    @DisplayName("기록은 있지만 모두 '안 마셨어요'인 경우 경험치 300을 지급한다")
    void 기록은_있는데_전부_안마신_유저는_경험치_300이_지급된다() {
        // given
        Long userId = 1L;

        UserGoalHistory goal = mock(UserGoalHistory.class);
        when(goal.getUserId()).thenReturn(userId);
        when(goal.getMonthlyGoalCount()).thenReturn(3);

        Puppy puppy = mock(Puppy.class);
        User user = mock(User.class);
        when(user.getUserId()).thenReturn(userId);
        when(puppy.getUser()).thenReturn(user);
        when(puppy.getPuppyExp()).thenReturn(100);

        UserDrinkCountProjection stat = mock(UserDrinkCountProjection.class);
        when(stat.getUserId()).thenReturn(userId);
        when(stat.getTotalCount()).thenReturn(5L);
        when(stat.getDrinkCount()).thenReturn(0L);

        when(userGoalHistoryRepository.findAllByGoalMonthAndRewardedFalse(any()))
                .thenReturn(List.of(goal));
        when(puppyRepository.findAllByUserUserIdIn(anyList()))
                .thenReturn(List.of(puppy));
        when(drinkHistoryRepository.countMonthlyDrinkByUserIds(anyList(), any(), any()))
                .thenReturn(List.of(stat));

        // when
        monthlyGoalScheduler.evaluateMonthlyGoals();

        // then
        verify(puppy).setPuppyExp(400);
        verify(goal).markRewarded();
    }
}