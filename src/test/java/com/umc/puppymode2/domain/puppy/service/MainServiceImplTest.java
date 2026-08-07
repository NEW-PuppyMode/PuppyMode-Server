package com.umc.puppymode2.domain.puppy.service;

import com.umc.puppymode2.domain.drinkhistory.repository.DrinkHistoryRepository;
import com.umc.puppymode2.domain.goal.repository.UserGoalHistoryRepository;
import com.umc.puppymode2.domain.puppy.dto.MainResponseDto;
import com.umc.puppymode2.domain.puppy.entity.*;
import com.umc.puppymode2.domain.puppy.repository.LevelExpRepository;
import com.umc.puppymode2.domain.puppy.repository.PuppyAppearanceRepository;
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
    private LevelExpRepository levelExpRepository;
    @Mock
    private PuppyAppearanceRepository puppyAppearanceRepository;
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

        Puppy puppy = Puppy.builder()
                .puppyType(PuppyType.BICHON)
                .puppyExp(50)
                .puppyName("테스트강아지")
                .build();

        LevelExp levelExp = mock(LevelExp.class);
        when(levelExp.getLevel()).thenReturn(1);
        when(levelExp.getMinExp()).thenReturn(0);
        when(levelExp.getMaxExp()).thenReturn(100);

        PuppyAppearance appearance = mock(PuppyAppearance.class);
        when(appearance.getStageName()).thenReturn("눈송이 비숑");
        when(appearance.getImageUrl()).thenReturn("url");

        when(userContext.getCurrentUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(puppyRepository.findByUser_UserId(userId)).thenReturn(Optional.of(puppy));
        when(levelExpRepository.findByExp(50)).thenReturn(Optional.of(levelExp));
        when(puppyAppearanceRepository.findByPuppyTypeAndLevel(PuppyType.BICHON, 1))
                .thenReturn(Optional.of(appearance));
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

    @Test
    void 구간_시작_exp면_퍼센트는_0이다() {
        // given
        User user = mock(User.class);

        Puppy puppy = Puppy.builder()
                .puppyType(PuppyType.BICHON)
                .puppyExp(945) // Lv19 구간(945~1045) 시작점
                .puppyName("테스트강아지")
                .build();

        LevelExp levelExp = mock(LevelExp.class);
        when(levelExp.getLevel()).thenReturn(19);
        when(levelExp.getMinExp()).thenReturn(945);
        when(levelExp.getMaxExp()).thenReturn(1045);

        PuppyAppearance appearance = mock(PuppyAppearance.class);
        when(appearance.getStageName()).thenReturn("곱슬 푸들");
        when(appearance.getImageUrl()).thenReturn("url");

        when(userContext.getCurrentUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(puppyRepository.findByUser_UserId(userId)).thenReturn(Optional.of(puppy));
        when(levelExpRepository.findByExp(945)).thenReturn(Optional.of(levelExp));
        when(puppyAppearanceRepository.findByPuppyTypeAndLevel(PuppyType.BICHON, 19))
                .thenReturn(Optional.of(appearance));
        when(userGoalHistoryRepository.findTopByUserIdOrderByGoalSetAtDesc(userId))
                .thenReturn(Optional.empty());

        // when
        MainResponseDto result = mainService.getMainPageInfo();

        // then
        assertEquals(0, result.getPuppyLevelPercent());
    }

    @Test
    void 구간_중간_exp면_퍼센트가_비율대로_계산된다() {
        // given
        User user = mock(User.class);

        Puppy puppy = Puppy.builder()
                .puppyType(PuppyType.BICHON)
                .puppyExp(1010) // Lv19 구간(945~1045) 안, 65% 지점
                .puppyName("테스트강아지")
                .build();

        LevelExp levelExp = mock(LevelExp.class);
        when(levelExp.getLevel()).thenReturn(19);
        when(levelExp.getMinExp()).thenReturn(945);
        when(levelExp.getMaxExp()).thenReturn(1045);

        PuppyAppearance appearance = mock(PuppyAppearance.class);
        when(appearance.getStageName()).thenReturn("곱슬 푸들");
        when(appearance.getImageUrl()).thenReturn("url");

        when(userContext.getCurrentUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(puppyRepository.findByUser_UserId(userId)).thenReturn(Optional.of(puppy));
        when(levelExpRepository.findByExp(1010)).thenReturn(Optional.of(levelExp));
        when(puppyAppearanceRepository.findByPuppyTypeAndLevel(PuppyType.BICHON, 19))
                .thenReturn(Optional.of(appearance));
        when(userGoalHistoryRepository.findTopByUserIdOrderByGoalSetAtDesc(userId))
                .thenReturn(Optional.empty());

        // when
        MainResponseDto result = mainService.getMainPageInfo();

        // then
        assertEquals(65, result.getPuppyLevelPercent());
    }

    @Test
    void 구간_끝_직전_exp면_퍼센트는_99이다() {
        // given
        User user = mock(User.class);

        Puppy puppy = Puppy.builder()
                .puppyType(PuppyType.BICHON)
                .puppyExp(1044) // Lv19 구간(945~1045) 끝 직전
                .puppyName("테스트강아지")
                .build();

        LevelExp levelExp = mock(LevelExp.class);
        when(levelExp.getLevel()).thenReturn(19);
        when(levelExp.getMinExp()).thenReturn(945);
        when(levelExp.getMaxExp()).thenReturn(1045);

        PuppyAppearance appearance = mock(PuppyAppearance.class);
        when(appearance.getStageName()).thenReturn("곱슬 푸들");
        when(appearance.getImageUrl()).thenReturn("url");

        when(userContext.getCurrentUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(puppyRepository.findByUser_UserId(userId)).thenReturn(Optional.of(puppy));
        when(levelExpRepository.findByExp(1044)).thenReturn(Optional.of(levelExp));
        when(puppyAppearanceRepository.findByPuppyTypeAndLevel(PuppyType.BICHON, 19))
                .thenReturn(Optional.of(appearance));
        when(userGoalHistoryRepository.findTopByUserIdOrderByGoalSetAtDesc(userId))
                .thenReturn(Optional.empty());

        // when
        MainResponseDto result = mainService.getMainPageInfo();

        // then
        assertEquals(99, result.getPuppyLevelPercent());
    }

    @Test
    void 레벨업하면_퍼센트가_0으로_초기화된다() {
        // given
        User user = mock(User.class);

        Puppy puppy = Puppy.builder()
                .puppyType(PuppyType.BICHON)
                .puppyExp(1045) // Lv20 구간(1045~1145) 시작점으로 넘어감
                .puppyName("테스트강아지")
                .build();

        LevelExp levelExp = mock(LevelExp.class);
        when(levelExp.getLevel()).thenReturn(20);
        when(levelExp.getMinExp()).thenReturn(1045);
        when(levelExp.getMaxExp()).thenReturn(1145);

        PuppyAppearance appearance = mock(PuppyAppearance.class);
        when(appearance.getStageName()).thenReturn("악성 곱슬 푸들");
        when(appearance.getImageUrl()).thenReturn("url");

        when(userContext.getCurrentUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(puppyRepository.findByUser_UserId(userId)).thenReturn(Optional.of(puppy));
        when(levelExpRepository.findByExp(1045)).thenReturn(Optional.of(levelExp));
        when(puppyAppearanceRepository.findByPuppyTypeAndLevel(PuppyType.BICHON, 20))
                .thenReturn(Optional.of(appearance));
        when(userGoalHistoryRepository.findTopByUserIdOrderByGoalSetAtDesc(userId))
                .thenReturn(Optional.empty());

        // when
        MainResponseDto result = mainService.getMainPageInfo();

        // then
        assertEquals(0, result.getPuppyLevelPercent());
    }

    @Test
    void 레벨30이면_퍼센트는_100이다() {
        // given
        User user = mock(User.class);

        Puppy puppy = Puppy.builder()
                .puppyType(PuppyType.BICHON)
                .puppyExp(2045)
                .puppyName("테스트강아지")
                .build();

        LevelExp levelExp = mock(LevelExp.class);
        when(levelExp.getLevel()).thenReturn(30);

        PuppyAppearance appearance = mock(PuppyAppearance.class);
        when(appearance.getStageName()).thenReturn("최종 형태");
        when(appearance.getImageUrl()).thenReturn("url");

        when(userContext.getCurrentUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(puppyRepository.findByUser_UserId(userId)).thenReturn(Optional.of(puppy));
        when(levelExpRepository.findByExp(2045)).thenReturn(Optional.of(levelExp));
        when(puppyAppearanceRepository.findByPuppyTypeAndLevel(PuppyType.BICHON, 30))
                .thenReturn(Optional.of(appearance));
        when(userGoalHistoryRepository.findTopByUserIdOrderByGoalSetAtDesc(userId))
                .thenReturn(Optional.empty());

        // when
        MainResponseDto result = mainService.getMainPageInfo();

        // then
        assertEquals(100, result.getPuppyLevelPercent());
    }
}