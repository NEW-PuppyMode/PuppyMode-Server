package com.umc.puppymode2.domain.puppy.service;

import com.umc.puppymode2.domain.drinkhistory.repository.DrinkHistoryRepository;
import com.umc.puppymode2.domain.goal.repository.UserGoalHistoryRepository;
import com.umc.puppymode2.domain.puppy.dto.MainResponseDto;
import com.umc.puppymode2.domain.puppy.entity.Puppy;
import com.umc.puppymode2.domain.puppy.entity.PuppyLevel;
import com.umc.puppymode2.domain.puppy.repository.PuppyRepository;
import com.umc.puppymode2.domain.user.entity.User;
import com.umc.puppymode2.domain.user.repository.UserRepository;
import com.umc.puppymode2.global.auth.context.UserContext;
import com.umc.puppymode2.global.exception.GeneralException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MainServiceImplTest {

    @Mock
    private PuppyRepository puppyRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private DrinkHistoryRepository drinkHistoryRepository;
    @Mock
    private UserGoalHistoryRepository userGoalHistoryRepository;
    @Mock
    private UserContext userContext;

    @InjectMocks
    private MainServiceImpl mainService;

    private final Long userId = 1L;

    @Test
    void 온보딩_미완료_유저가_메인을_조회하면_isOnboarded가_false이다() {
        // given
        User user = mock(User.class);

        when(userContext.getCurrentUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(puppyRepository.findByUser_UserId(userId)).thenReturn(Optional.empty());

        // when
        MainResponseDto result = mainService.getMainPageInfo();

        // then
        assertFalse(result.isOnboarded());
    }

    @Test
    void 온보딩_완료_유저가_메인을_조회하면_isOnboarded가_true이다() {
        // given
        User user = mock(User.class);

        PuppyLevel level = PuppyLevel.builder()
                .puppyLevel(1)
                .levelMinExp(0L)
                .levelMaxExp(100L)
                .build();

        Puppy puppy = Puppy.builder()
                .puppyLevel(level)
                .puppyExp(50)
                .puppyName("테스트강아지")
                .build();

        when(userContext.getCurrentUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(puppyRepository.findByUser_UserId(userId)).thenReturn(Optional.of(puppy));
        when(userGoalHistoryRepository.findTopByUserIdOrderByGoalSetAtDesc(userId))
                .thenReturn(Optional.empty());

        // when
        MainResponseDto result = mainService.getMainPageInfo();

        // then
        assertTrue(result.isOnboarded());
    }

    @Test
    void 존재하지_않는_유저가_메인을_조회하면_예외가_발생한다() {
        // given
        when(userContext.getCurrentUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when & then
        assertThrows(GeneralException.class,
                () -> mainService.getMainPageInfo());
    }
}