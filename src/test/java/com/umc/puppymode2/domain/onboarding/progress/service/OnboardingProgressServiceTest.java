package com.umc.puppymode2.domain.onboarding.progress.service;

import com.umc.puppymode2.domain.goal.repository.UserGoalHistoryRepository;
import com.umc.puppymode2.domain.onboarding.progress.OnboardingProgressService;
import com.umc.puppymode2.domain.puppy.entity.Puppy;
import com.umc.puppymode2.domain.puppy.repository.PuppyRepository;
import com.umc.puppymode2.domain.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OnboardingProgressServiceTest {

    @Mock
    private PuppyRepository puppyRepository;
    @Mock
    private UserGoalHistoryRepository userGoalHistoryRepository;

    @InjectMocks
    private OnboardingProgressService onboardingProgressService;

    private final Long userId = 1L;

    @Test
    void 강아지_검사_자체를_안한_경우_온보딩_미완료다() {
        // given
        User user = mock(User.class);
        when(user.getUserId()).thenReturn(userId);
        when(puppyRepository.findByUser_UserId(userId)).thenReturn(Optional.empty());

        // when
        boolean result = onboardingProgressService.isOnboardingCompleted(user);

        // then
        assertFalse(result);
    }

    @Test
    void 강아지_이름을_아직_안지은_경우_온보딩_미완료다() {
        // given
        User user = mock(User.class);
        Puppy puppy = mock(Puppy.class);

        when(user.getUserId()).thenReturn(userId);
        when(puppyRepository.findByUser_UserId(userId)).thenReturn(Optional.of(puppy));
        when(puppy.isCustomName()).thenReturn(false);

        // when
        boolean result = onboardingProgressService.isOnboardingCompleted(user);

        // then
        assertFalse(result);
    }

    @Test
    void 내이름을_아직_안지은_경우_온보딩_미완료다() {
        // given
        User user = mock(User.class);
        Puppy puppy = mock(Puppy.class);

        when(user.getUserId()).thenReturn(userId);
        when(puppyRepository.findByUser_UserId(userId)).thenReturn(Optional.of(puppy));
        when(puppy.isCustomName()).thenReturn(true);
        when(user.isCustomName()).thenReturn(false);

        // when
        boolean result = onboardingProgressService.isOnboardingCompleted(user);

        // then
        assertFalse(result);
    }

    @Test
    void 목표_이력은_있지만_강아지이름_내이름을_아직_안지은_경우_온보딩_미완료다() {
        // given
        User user = mock(User.class);
        Puppy puppy = mock(Puppy.class);

        when(user.getUserId()).thenReturn(userId);
        when(puppyRepository.findByUser_UserId(userId)).thenReturn(Optional.of(puppy));
        when(puppy.isCustomName()).thenReturn(false); // 강아지이름 아직

        // when
        boolean result = onboardingProgressService.isOnboardingCompleted(user);

        // then
        assertFalse(result);

        verify(userGoalHistoryRepository, never()).existsByUserId(anyLong());
    }

    @Test
    void 이름은_다_지었지만_목표_이력이_없는_경우_온보딩_미완료다() {
        // given
        User user = mock(User.class);
        Puppy puppy = mock(Puppy.class);

        when(user.getUserId()).thenReturn(userId);
        when(puppyRepository.findByUser_UserId(userId)).thenReturn(Optional.of(puppy));
        when(puppy.isCustomName()).thenReturn(true);
        when(user.isCustomName()).thenReturn(true);
        when(userGoalHistoryRepository.existsByUserId(userId)).thenReturn(false);

        // when
        boolean result = onboardingProgressService.isOnboardingCompleted(user);

        // then
        assertFalse(result);
    }

    @Test
    void 강아지이름_내이름_목표_모두_완료된_경우_온보딩_완료다() {
        // given
        User user = mock(User.class);
        Puppy puppy = mock(Puppy.class);

        when(user.getUserId()).thenReturn(userId);
        when(puppyRepository.findByUser_UserId(userId)).thenReturn(Optional.of(puppy));
        when(puppy.isCustomName()).thenReturn(true);
        when(user.isCustomName()).thenReturn(true);
        when(userGoalHistoryRepository.existsByUserId(userId)).thenReturn(true);

        // when
        boolean result = onboardingProgressService.isOnboardingCompleted(user);

        // then
        assertTrue(result);
    }
}