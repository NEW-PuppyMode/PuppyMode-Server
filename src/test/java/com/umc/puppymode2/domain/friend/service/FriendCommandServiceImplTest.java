package com.umc.puppymode2.domain.friend.service;

import com.umc.puppymode2.domain.friend.converter.FriendConverter;
import com.umc.puppymode2.domain.friend.dto.FriendRequestAcceptResponseDTO;
import com.umc.puppymode2.domain.friend.dto.FriendRequestSendResponseDTO;
import com.umc.puppymode2.domain.friend.entity.FriendCode;
import com.umc.puppymode2.domain.friend.entity.FriendRequest;
import com.umc.puppymode2.domain.friend.entity.Friendship;
import com.umc.puppymode2.domain.friend.entity.enums.FriendRequestStatus;
import com.umc.puppymode2.domain.friend.exception.FriendErrorStatus;
import com.umc.puppymode2.domain.friend.repository.FriendCodeRepository;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FriendCommandServiceImplTest {

    @Mock private FriendCodeRepository friendCodeRepository;
    @Mock private FriendRequestRepository friendRequestRepository;
    @Mock private FriendshipRepository friendshipRepository;
    @Mock private UserRepository userRepository;
    @Mock private FriendCodeAttemptLimiter attemptLimiter;
    @Spy private FriendConverter converter = new FriendConverter();

    @InjectMocks
    private FriendCommandServiceImpl service;

    private static final Long ME = 10L;
    private static final Long OTHER = 20L;
    private static final String CODE = "4829";

    // ---------------------------------------------------------------- 친구 요청 보내기

    @Test
    void 친구_요청_성공시_PENDING_요청을_새로_만든다() {
        givenTargetExists(OTHER, UserStatus.NORMAL);
        when(friendshipRepository.existsByUserLowIdAndUserHighId(ME, OTHER)).thenReturn(false);
        when(friendRequestRepository.findByRequesterIdAndReceiverId(OTHER, ME)).thenReturn(Optional.empty());
        when(friendRequestRepository.findByRequesterIdAndReceiverId(ME, OTHER)).thenReturn(Optional.empty());
        when(friendRequestRepository.saveAndFlush(any(FriendRequest.class))).thenAnswer(inv -> {
            FriendRequest saved = inv.getArgument(0);
            ReflectionTestUtils.setField(saved, "friendRequestId", 101L);
            return saved;
        });

        FriendRequestSendResponseDTO result = service.sendFriendRequest(ME, CODE);

        assertEquals(101L, result.getRequestId());
        assertEquals(FriendRequestStatus.PENDING, result.getStatus());
        assertFalse(result.isAutoAccepted());
        verify(attemptLimiter).assertNotLimited(ME);
        verify(attemptLimiter, never()).recordFailure(any());
    }

    @Test
    void 없는_코드는_404이고_실패_횟수를_기록한다() {
        when(friendCodeRepository.findByCode(CODE)).thenReturn(Optional.empty());

        GeneralException e = assertThrows(GeneralException.class, () -> service.sendFriendRequest(ME, CODE));

        assertEquals(FriendErrorStatus.FRIEND_CODE_NOT_FOUND, e.getCode());
        verify(attemptLimiter).recordFailure(ME);
    }

    @Test
    void 상대가_NORMAL이_아니면_없는_코드와_동일하게_404() {
        givenTargetExists(OTHER, UserStatus.REST);

        GeneralException e = assertThrows(GeneralException.class, () -> service.sendFriendRequest(ME, CODE));

        assertEquals(FriendErrorStatus.FRIEND_CODE_NOT_FOUND, e.getCode());
        verify(attemptLimiter).recordFailure(ME);
    }

    @Test
    void 탈퇴한_사용자의_코드는_404() {
        givenTargetExists(OTHER, UserStatus.STOP);

        GeneralException e = assertThrows(GeneralException.class, () -> service.sendFriendRequest(ME, CODE));

        assertEquals(FriendErrorStatus.FRIEND_CODE_NOT_FOUND, e.getCode());
    }

    @Test
    void 내_코드를_입력하면_400이고_실패_횟수를_기록한다() {
        givenTargetExists(ME, UserStatus.NORMAL);

        GeneralException e = assertThrows(GeneralException.class, () -> service.sendFriendRequest(ME, CODE));

        assertEquals(FriendErrorStatus.CANNOT_ADD_SELF, e.getCode());
        verify(attemptLimiter).recordFailure(ME);
    }

    @Test
    void 이미_친구면_409이고_실패_횟수는_기록하지_않는다() {
        givenTargetExists(OTHER, UserStatus.NORMAL);
        when(friendshipRepository.existsByUserLowIdAndUserHighId(ME, OTHER)).thenReturn(true);

        GeneralException e = assertThrows(GeneralException.class, () -> service.sendFriendRequest(ME, CODE));

        assertEquals(FriendErrorStatus.ALREADY_FRIENDS, e.getCode());
        verify(attemptLimiter, never()).recordFailure(any());
    }

    @Test
    void 친구_요청은_ID_순서와_무관하게_low_high로_정규화해_친구를_판정한다() {
        // 내 ID가 상대보다 큰 경우에도 (small, large) 순서로 조회해야 한다.
        Long bigMe = 99L;
        givenTargetExists(OTHER, UserStatus.NORMAL);
        when(friendshipRepository.existsByUserLowIdAndUserHighId(OTHER, bigMe)).thenReturn(true);

        GeneralException e = assertThrows(GeneralException.class, () -> service.sendFriendRequest(bigMe, CODE));

        assertEquals(FriendErrorStatus.ALREADY_FRIENDS, e.getCode());
    }

    @Test
    void 이미_요청한_상태에서_다시_요청하면_409() {
        givenTargetExists(OTHER, UserStatus.NORMAL);
        when(friendshipRepository.existsByUserLowIdAndUserHighId(ME, OTHER)).thenReturn(false);
        when(friendRequestRepository.findByRequesterIdAndReceiverId(OTHER, ME)).thenReturn(Optional.empty());
        when(friendRequestRepository.findByRequesterIdAndReceiverId(ME, OTHER))
                .thenReturn(Optional.of(request(101L, ME, OTHER, FriendRequestStatus.PENDING)));

        GeneralException e = assertThrows(GeneralException.class, () -> service.sendFriendRequest(ME, CODE));

        assertEquals(FriendErrorStatus.REQUEST_ALREADY_SENT, e.getCode());
        verify(friendRequestRepository, never()).saveAndFlush(any());
    }

    @Test
    void 거절당한_뒤_재요청하면_기존_요청을_PENDING으로_되돌린다() {
        FriendRequest rejected = request(101L, ME, OTHER, FriendRequestStatus.REJECTED);
        givenTargetExists(OTHER, UserStatus.NORMAL);
        when(friendshipRepository.existsByUserLowIdAndUserHighId(ME, OTHER)).thenReturn(false);
        when(friendRequestRepository.findByRequesterIdAndReceiverId(OTHER, ME)).thenReturn(Optional.empty());
        when(friendRequestRepository.findByRequesterIdAndReceiverId(ME, OTHER)).thenReturn(Optional.of(rejected));

        FriendRequestSendResponseDTO result = service.sendFriendRequest(ME, CODE);

        assertEquals(101L, result.getRequestId());
        assertEquals(FriendRequestStatus.PENDING, result.getStatus());
        assertTrue(rejected.isPending());
        assertNull(rejected.getRespondedAt());
        verify(friendRequestRepository, never()).saveAndFlush(any());
    }

    @Test
    void 친구_삭제_후_재요청하면_수락됐던_기존_요청을_PENDING으로_되돌린다() {
        FriendRequest accepted = request(101L, ME, OTHER, FriendRequestStatus.ACCEPTED);
        givenTargetExists(OTHER, UserStatus.NORMAL);
        when(friendshipRepository.existsByUserLowIdAndUserHighId(ME, OTHER)).thenReturn(false);
        when(friendRequestRepository.findByRequesterIdAndReceiverId(OTHER, ME)).thenReturn(Optional.empty());
        when(friendRequestRepository.findByRequesterIdAndReceiverId(ME, OTHER)).thenReturn(Optional.of(accepted));

        FriendRequestSendResponseDTO result = service.sendFriendRequest(ME, CODE);

        assertEquals(FriendRequestStatus.PENDING, result.getStatus());
        assertTrue(accepted.isPending());
    }

    @Test
    void 상대가_이미_나에게_요청했으면_자동_수락되어_친구가_된다() {
        // 서로 요청한 경우(상호 요청): 상대→나 PENDING 이 있으면 새 요청을 만들지 않고 그 요청을 수락한다.
        FriendRequest incoming = request(55L, OTHER, ME, FriendRequestStatus.PENDING);
        givenTargetExists(OTHER, UserStatus.NORMAL);
        when(friendshipRepository.existsByUserLowIdAndUserHighId(ME, OTHER)).thenReturn(false);
        when(friendRequestRepository.findByRequesterIdAndReceiverId(OTHER, ME)).thenReturn(Optional.of(incoming));

        FriendRequestSendResponseDTO result = service.sendFriendRequest(ME, CODE);

        assertTrue(result.isAutoAccepted());
        assertEquals(FriendRequestStatus.ACCEPTED, result.getStatus());
        assertEquals(55L, result.getRequestId());
        assertEquals(FriendRequestStatus.ACCEPTED, incoming.getStatus());

        ArgumentCaptor<Friendship> captor = ArgumentCaptor.forClass(Friendship.class);
        verify(friendshipRepository).saveAndFlush(captor.capture());
        assertEquals(ME, captor.getValue().getUserLowId());
        assertEquals(OTHER, captor.getValue().getUserHighId());
        // 내가 새로 보내는 요청 행은 만들지 않는다.
        verify(friendRequestRepository, never()).saveAndFlush(any(FriendRequest.class));
    }

    @Test
    void 자동_수락_중_다른_경로로_이미_친구가_됐다면_409() {
        FriendRequest incoming = request(55L, OTHER, ME, FriendRequestStatus.PENDING);
        givenTargetExists(OTHER, UserStatus.NORMAL);
        when(friendshipRepository.existsByUserLowIdAndUserHighId(ME, OTHER)).thenReturn(false);
        when(friendRequestRepository.findByRequesterIdAndReceiverId(OTHER, ME)).thenReturn(Optional.of(incoming));
        when(friendshipRepository.saveAndFlush(any(Friendship.class)))
                .thenThrow(new DataIntegrityViolationException("uk_friendship_pair"));

        GeneralException e = assertThrows(GeneralException.class, () -> service.sendFriendRequest(ME, CODE));

        assertEquals(FriendErrorStatus.ALREADY_FRIENDS, e.getCode());
    }

    @Test
    void 더블탭으로_동시에_같은_요청이_들어와_UNIQUE_위반이면_이미_요청함으로_처리한다() {
        givenTargetExists(OTHER, UserStatus.NORMAL);
        when(friendshipRepository.existsByUserLowIdAndUserHighId(ME, OTHER)).thenReturn(false);
        when(friendRequestRepository.findByRequesterIdAndReceiverId(OTHER, ME)).thenReturn(Optional.empty());
        when(friendRequestRepository.findByRequesterIdAndReceiverId(ME, OTHER)).thenReturn(Optional.empty());
        when(friendRequestRepository.saveAndFlush(any(FriendRequest.class)))
                .thenThrow(new DataIntegrityViolationException("uk_friend_request_pair"));

        GeneralException e = assertThrows(GeneralException.class, () -> service.sendFriendRequest(ME, CODE));

        assertEquals(FriendErrorStatus.REQUEST_ALREADY_SENT, e.getCode());
    }

    @Test
    void 실패_한도를_넘은_사용자는_코드_조회_전에_429로_막힌다() {
        doThrow(new GeneralException(FriendErrorStatus.TOO_MANY_ATTEMPTS)).when(attemptLimiter).assertNotLimited(ME);

        GeneralException e = assertThrows(GeneralException.class, () -> service.sendFriendRequest(ME, CODE));

        assertEquals(FriendErrorStatus.TOO_MANY_ATTEMPTS, e.getCode());
        verifyNoInteractions(friendCodeRepository);
    }

    // ---------------------------------------------------------------- 수락

    @Test
    void 수락하면_요청이_ACCEPTED가_되고_친구_관계가_생성된다() {
        FriendRequest pending = request(101L, OTHER, ME, FriendRequestStatus.PENDING);
        when(friendRequestRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(pending));
        when(userRepository.findById(OTHER)).thenReturn(Optional.of(user(OTHER, UserStatus.NORMAL)));
        when(friendRequestRepository.findByRequesterIdAndReceiverId(ME, OTHER)).thenReturn(Optional.empty());

        FriendRequestAcceptResponseDTO result = service.acceptFriendRequest(ME, 101L);

        assertEquals(OTHER, result.getFriendUserId());
        assertEquals(FriendRequestStatus.ACCEPTED, pending.getStatus());
        assertNotNull(pending.getRespondedAt());

        ArgumentCaptor<Friendship> captor = ArgumentCaptor.forClass(Friendship.class);
        verify(friendshipRepository).saveAndFlush(captor.capture());
        assertEquals(ME, captor.getValue().getUserLowId());
        assertEquals(OTHER, captor.getValue().getUserHighId());
    }

    @Test
    void 수락하면_서로_동시에_보내_남아있던_반대_방향_PENDING_요청도_함께_수락된다() {
        FriendRequest incoming = request(101L, OTHER, ME, FriendRequestStatus.PENDING);
        FriendRequest myOutgoing = request(102L, ME, OTHER, FriendRequestStatus.PENDING);
        when(friendRequestRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(incoming));
        when(userRepository.findById(OTHER)).thenReturn(Optional.of(user(OTHER, UserStatus.NORMAL)));
        when(friendRequestRepository.findByRequesterIdAndReceiverId(ME, OTHER)).thenReturn(Optional.of(myOutgoing));

        service.acceptFriendRequest(ME, 101L);

        assertEquals(FriendRequestStatus.ACCEPTED, myOutgoing.getStatus());
    }

    @Test
    void 없는_요청을_수락하면_404() {
        when(friendRequestRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

        GeneralException e = assertThrows(GeneralException.class, () -> service.acceptFriendRequest(ME, 999L));

        assertEquals(FriendErrorStatus.REQUEST_NOT_FOUND, e.getCode());
    }

    @Test
    void 내가_받은_요청이_아니면_403() {
        // 요청자와 수신자가 모두 내가 아닌 요청
        FriendRequest others = request(101L, OTHER, 30L, FriendRequestStatus.PENDING);
        when(friendRequestRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(others));

        GeneralException e = assertThrows(GeneralException.class, () -> service.acceptFriendRequest(ME, 101L));

        assertEquals(FriendErrorStatus.NOT_REQUEST_RECEIVER, e.getCode());
        verify(friendshipRepository, never()).saveAndFlush(any());
    }

    @Test
    void 내가_보낸_요청을_내가_수락하려_하면_403() {
        FriendRequest mine = request(101L, ME, OTHER, FriendRequestStatus.PENDING);
        when(friendRequestRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(mine));

        GeneralException e = assertThrows(GeneralException.class, () -> service.acceptFriendRequest(ME, 101L));

        assertEquals(FriendErrorStatus.NOT_REQUEST_RECEIVER, e.getCode());
    }

    @Test
    void 이미_처리된_요청을_다시_수락하면_409() {
        FriendRequest accepted = request(101L, OTHER, ME, FriendRequestStatus.ACCEPTED);
        when(friendRequestRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(accepted));

        GeneralException e = assertThrows(GeneralException.class, () -> service.acceptFriendRequest(ME, 101L));

        assertEquals(FriendErrorStatus.REQUEST_ALREADY_HANDLED, e.getCode());
    }

    @Test
    void 요청자가_그_사이_탈퇴했다면_수락은_없는_요청으로_404() {
        FriendRequest pending = request(101L, OTHER, ME, FriendRequestStatus.PENDING);
        when(friendRequestRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(pending));
        when(userRepository.findById(OTHER)).thenReturn(Optional.of(user(OTHER, UserStatus.STOP)));

        GeneralException e = assertThrows(GeneralException.class, () -> service.acceptFriendRequest(ME, 101L));

        assertEquals(FriendErrorStatus.REQUEST_NOT_FOUND, e.getCode());
        verify(friendshipRepository, never()).saveAndFlush(any());
    }

    @Test
    void 동시에_수락해_친구_관계가_이미_있으면_이미_처리된_요청으로_409() {
        FriendRequest pending = request(101L, OTHER, ME, FriendRequestStatus.PENDING);
        when(friendRequestRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(pending));
        when(userRepository.findById(OTHER)).thenReturn(Optional.of(user(OTHER, UserStatus.NORMAL)));
        when(friendshipRepository.saveAndFlush(any(Friendship.class)))
                .thenThrow(new DataIntegrityViolationException("uk_friendship_pair"));

        GeneralException e = assertThrows(GeneralException.class, () -> service.acceptFriendRequest(ME, 101L));

        assertEquals(FriendErrorStatus.REQUEST_ALREADY_HANDLED, e.getCode());
    }

    // ---------------------------------------------------------------- 거절

    @Test
    void 거절하면_REJECTED가_되고_친구_관계는_만들지_않는다() {
        FriendRequest pending = request(101L, OTHER, ME, FriendRequestStatus.PENDING);
        when(friendRequestRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(pending));

        service.rejectFriendRequest(ME, 101L);

        assertEquals(FriendRequestStatus.REJECTED, pending.getStatus());
        verify(friendshipRepository, never()).saveAndFlush(any());
    }

    @Test
    void 이미_처리된_요청을_거절하면_409() {
        FriendRequest rejected = request(101L, OTHER, ME, FriendRequestStatus.REJECTED);
        when(friendRequestRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(rejected));

        GeneralException e = assertThrows(GeneralException.class, () -> service.rejectFriendRequest(ME, 101L));

        assertEquals(FriendErrorStatus.REQUEST_ALREADY_HANDLED, e.getCode());
    }

    @Test
    void 내가_받은_요청이_아니면_거절도_403() {
        FriendRequest others = request(101L, OTHER, 30L, FriendRequestStatus.PENDING);
        when(friendRequestRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(others));

        GeneralException e = assertThrows(GeneralException.class, () -> service.rejectFriendRequest(ME, 101L));

        assertEquals(FriendErrorStatus.NOT_REQUEST_RECEIVER, e.getCode());
    }

    // ---------------------------------------------------------------- 친구 삭제

    @Test
    void 친구를_삭제하면_친구_관계_행이_삭제된다() {
        Friendship friendship = Friendship.of(ME, OTHER);
        when(friendshipRepository.findByUserLowIdAndUserHighId(ME, OTHER)).thenReturn(Optional.of(friendship));

        service.deleteFriend(ME, OTHER);

        verify(friendshipRepository).delete(friendship);
    }

    @Test
    void 친구가_아닌_사용자를_삭제하면_404() {
        when(friendshipRepository.findByUserLowIdAndUserHighId(ME, OTHER)).thenReturn(Optional.empty());

        GeneralException e = assertThrows(GeneralException.class, () -> service.deleteFriend(ME, OTHER));

        assertEquals(FriendErrorStatus.FRIENDSHIP_NOT_FOUND, e.getCode());
        verify(friendshipRepository, never()).delete(any());
    }

    // ---------------------------------------------------------------- 픽스처

    private void givenTargetExists(Long targetUserId, UserStatus status) {
        when(friendCodeRepository.findByCode(CODE)).thenReturn(Optional.of(FriendCode.of(targetUserId, CODE)));
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(user(targetUserId, status)));
    }

    private User user(Long userId, UserStatus status) {
        User user = User.builder()
                .username("user" + userId)
                .email("user" + userId + "@test.com")
                .provider(Provider.KAKAO)
                .status(status)
                .build();
        ReflectionTestUtils.setField(user, "userId", userId);
        return user;
    }

    private FriendRequest request(Long id, Long requesterId, Long receiverId, FriendRequestStatus status) {
        FriendRequest request = FriendRequest.create(requesterId, receiverId);
        ReflectionTestUtils.setField(request, "friendRequestId", id);
        ReflectionTestUtils.setField(request, "status", status);
        return request;
    }
}
