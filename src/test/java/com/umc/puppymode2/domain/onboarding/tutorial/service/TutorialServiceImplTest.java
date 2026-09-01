package com.umc.puppymode2.domain.onboarding.tutorial.service;

import com.umc.puppymode2.domain.user.entity.User;
import com.umc.puppymode2.domain.user.repository.UserRepository;
import com.umc.puppymode2.global.exception.GeneralException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TutorialServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TutorialServiceImpl tutorialService;

    private final Long userId = 1L;

    @Test
    void 처음_호출하면_tutorialShown이_true로_설정된다() {
        // given
        User user = mock(User.class);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(user.isTutorialShown()).thenReturn(false);

        // when
        tutorialService.markTutorialShown(userId);

        // then
        verify(user).setTutorialShown(true);
    }

    @Test
    void 이미_true인_상태에서_재호출해도_에러없이_통과한다() {
        // given: 멱등성 확인 - 이미 봤음 상태에서 또 호출
        User user = mock(User.class);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(user.isTutorialShown()).thenReturn(true);

        // when & then
        assertDoesNotThrow(() -> tutorialService.markTutorialShown(userId));

        // 이미 true이므로 불필요한 재저장 호출은 없어야 함
        verify(user, never()).setTutorialShown(anyBoolean());
    }

    @Test
    void 존재하지_않는_유저면_예외가_발생한다() {
        // given
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when & then
        assertThrows(GeneralException.class,
                () -> tutorialService.markTutorialShown(userId));
    }
}