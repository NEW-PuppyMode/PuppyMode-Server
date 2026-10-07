package com.umc.puppymode2.domain.block.service;

import com.umc.puppymode2.domain.block.dto.BlockCreateResponseDTO;
import com.umc.puppymode2.domain.block.entity.UserBlock;
import com.umc.puppymode2.domain.block.exception.BlockErrorStatus;
import com.umc.puppymode2.domain.block.repository.UserBlockRepository;
import com.umc.puppymode2.domain.cheer.repository.CheerRepository;
import com.umc.puppymode2.domain.friend.converter.FriendConverter;
import com.umc.puppymode2.domain.friend.entity.Friendship;
import com.umc.puppymode2.domain.friend.repository.FriendRequestRepository;
import com.umc.puppymode2.domain.friend.repository.FriendshipRepository;
import com.umc.puppymode2.domain.user.auth.enums.Provider;
import com.umc.puppymode2.domain.user.entity.User;
import com.umc.puppymode2.domain.user.entity.enums.UserStatus;
import com.umc.puppymode2.domain.user.repository.UserRepository;
import com.umc.puppymode2.global.exception.GeneralException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BlockCommandServiceImplTest {

    @Mock private UserBlockRepository userBlockRepository;
    @Mock private FriendshipRepository friendshipRepository;
    @Mock private FriendRequestRepository friendRequestRepository;
    @Mock private CheerRepository cheerRepository;
    @Mock private UserRepository userRepository;
    @Spy private FriendConverter friendConverter = new FriendConverter();

    @InjectMocks
    private BlockCommandServiceImpl service;

    private static final Long ME = 30L;   // low/high 정규화가 동작하는지 보려고 ME > OTHER 로 둔다
    private static final Long OTHER = 20L;

    private void assertError(BlockErrorStatus expected, Runnable action) {
        GeneralException e = assertThrows(GeneralException.class, action::run);
        assertEquals(expected, e.getCode());
    }

    private User user(Long id, UserStatus status) {
        User user = User.builder()
                .username("user" + id)
                .email("user" + id + "@test.com")
                .provider(Provider.KAKAO)
                .status(status)
                .build();
        ReflectionTestUtils.setField(user, "userId", id);
        return user;
    }

    private void givenTarget(UserStatus status) {
        when(userRepository.findById(OTHER)).thenReturn(Optional.of(user(OTHER, status)));
    }

    private void givenBlockSaved() {
        when(userBlockRepository.saveAndFlush(any(UserBlock.class))).thenAnswer(inv -> {
            UserBlock block = inv.getArgument(0);
            ReflectionTestUtils.setField(block, "createdAt", LocalDateTime.of(2026, 10, 7, 10, 12));
            return block;
        });
    }

    // ---------------------------------------------------------------- 차단하기

    @Test
    void 친구를_차단하면_차단_기록을_저장하고_친구_응원_대기_요청을_함께_지운다() {
        givenTarget(UserStatus.NORMAL);
        givenBlockSaved();
        Friendship friendship = Friendship.of(ME, OTHER);
        when(friendshipRepository.findByUserLowIdAndUserHighId(OTHER, ME)).thenReturn(Optional.of(friendship));

        BlockCreateResponseDTO result = service.block(ME, OTHER);

        assertEquals(OTHER, result.getUserId());
        assertNotNull(result.getBlockedAt());

        ArgumentCaptor<UserBlock> saved = ArgumentCaptor.forClass(UserBlock.class);
        verify(userBlockRepository).saveAndFlush(saved.capture());
        assertEquals(ME, saved.getValue().getBlockerId());
        assertEquals(OTHER, saved.getValue().getBlockedId());

        // 차단 기록을 먼저 저장해 동시 중복 요청을 직렬화한 뒤 삭제한다.
        InOrder order = inOrder(userBlockRepository, friendshipRepository, cheerRepository, friendRequestRepository);
        order.verify(userBlockRepository).saveAndFlush(any(UserBlock.class));
        order.verify(friendshipRepository).delete(friendship);
        order.verify(cheerRepository).deleteAllBetween(ME, OTHER);
        order.verify(friendRequestRepository).deletePendingBetween(ME, OTHER);
    }

    @Test
    void 자기_자신은_차단할_수_없다() {
        assertError(BlockErrorStatus.CANNOT_BLOCK_SELF, () -> service.block(ME, ME));

        verifyNoInteractions(userBlockRepository, friendshipRepository, cheerRepository, friendRequestRepository);
    }

    @Test
    void 없는_사용자는_친구가_아닌_것과_같이_403() {
        when(userRepository.findById(OTHER)).thenReturn(Optional.empty());

        assertError(BlockErrorStatus.NOT_FRIENDS, () -> service.block(ME, OTHER));

        verify(userBlockRepository, never()).saveAndFlush(any());
    }

    @Test
    void NORMAL이_아닌_사용자도_친구가_아닌_것과_같이_403() {
        givenTarget(UserStatus.STOP);

        assertError(BlockErrorStatus.NOT_FRIENDS, () -> service.block(ME, OTHER));

        verify(userBlockRepository, never()).saveAndFlush(any());
    }

    @Test
    void 이미_차단했다면_친구_확인보다_먼저_409() {
        givenTarget(UserStatus.NORMAL);
        when(userBlockRepository.existsByBlockerIdAndBlockedId(ME, OTHER)).thenReturn(true);

        assertError(BlockErrorStatus.ALREADY_BLOCKED, () -> service.block(ME, OTHER));

        verify(userBlockRepository, never()).saveAndFlush(any());
        verifyNoInteractions(friendshipRepository, cheerRepository, friendRequestRepository);
    }

    @Test
    void 동시_요청으로_UNIQUE에_걸리면_이미_차단한_것으로_409이고_아무것도_지우지_않는다() {
        givenTarget(UserStatus.NORMAL);
        when(userBlockRepository.saveAndFlush(any(UserBlock.class)))
                .thenThrow(new DataIntegrityViolationException("uk_user_block_pair"));

        assertError(BlockErrorStatus.ALREADY_BLOCKED, () -> service.block(ME, OTHER));

        verifyNoInteractions(friendshipRepository, cheerRepository, friendRequestRepository);
    }

    @Test
    void 친구가_아니면_403이고_예외로_차단_기록_저장도_롤백된다() {
        givenTarget(UserStatus.NORMAL);
        givenBlockSaved();
        when(friendshipRepository.findByUserLowIdAndUserHighId(OTHER, ME)).thenReturn(Optional.empty());

        // 서비스 메서드가 @Transactional이라 이 예외로 앞에서 저장한 차단 기록이 롤백된다. (실제 롤백은 통합 테스트에서 검증)
        assertError(BlockErrorStatus.NOT_FRIENDS, () -> service.block(ME, OTHER));

        verify(friendshipRepository, never()).delete(any());
        verifyNoInteractions(cheerRepository, friendRequestRepository);
    }

    // ---------------------------------------------------------------- 차단 해제

    @Test
    void 차단을_해제하면_차단_기록만_지우고_친구는_복구하지_않는다() {
        when(userBlockRepository.deleteByBlockerAndBlocked(ME, OTHER)).thenReturn(1);

        service.unblock(ME, OTHER);

        verify(userBlockRepository).deleteByBlockerAndBlocked(ME, OTHER);
        verifyNoInteractions(friendshipRepository, cheerRepository, friendRequestRepository);
    }

    @Test
    void 차단한_기록이_없으면_해제는_404() {
        when(userBlockRepository.deleteByBlockerAndBlocked(ME, OTHER)).thenReturn(0);

        assertError(BlockErrorStatus.BLOCK_NOT_FOUND, () -> service.unblock(ME, OTHER));
    }
}
