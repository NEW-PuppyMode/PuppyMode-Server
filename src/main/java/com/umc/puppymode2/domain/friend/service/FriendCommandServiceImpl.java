package com.umc.puppymode2.domain.friend.service;

import com.umc.puppymode2.domain.friend.converter.FriendConverter;
import com.umc.puppymode2.domain.friend.dto.FriendRequestAcceptResponseDTO;
import com.umc.puppymode2.domain.friend.dto.FriendRequestSendResponseDTO;
import com.umc.puppymode2.domain.friend.entity.FriendCode;
import com.umc.puppymode2.domain.friend.entity.FriendRequest;
import com.umc.puppymode2.domain.friend.entity.Friendship;
import com.umc.puppymode2.domain.friend.exception.FriendErrorStatus;
import com.umc.puppymode2.domain.friend.repository.FriendCodeRepository;
import com.umc.puppymode2.domain.friend.repository.FriendRequestRepository;
import com.umc.puppymode2.domain.friend.repository.FriendshipRepository;
import com.umc.puppymode2.domain.user.entity.User;
import com.umc.puppymode2.domain.user.entity.enums.UserStatus;
import com.umc.puppymode2.domain.user.repository.UserRepository;
import com.umc.puppymode2.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FriendCommandServiceImpl implements FriendCommandService {

    private final FriendCodeRepository friendCodeRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final FriendCodeAttemptLimiter attemptLimiter;
    private final FriendConverter converter;

    @Transactional
    @Override
    public FriendRequestSendResponseDTO sendFriendRequest(Long myUserId, String friendCode) {

        // 0. 코드 입력 실패가 한도를 넘은 사용자는 코드 조회 자체를 막는다. (무작위 대입 방지)
        attemptLimiter.assertNotLimited(myUserId);

        // 1. 코드로 상대 조회. 없는 코드이거나 상대가 NORMAL이 아니면 "존재하지 않는 코드"로 응답한다.
        FriendCode targetCode = friendCodeRepository.findByCode(friendCode)
                .orElseThrow(() -> codeInputFailure(myUserId, FriendErrorStatus.FRIEND_CODE_NOT_FOUND));
        Long targetUserId = targetCode.getUserId();

        userRepository.findById(targetUserId)
                .filter(user -> user.getStatus() == UserStatus.NORMAL)
                .orElseThrow(() -> codeInputFailure(myUserId, FriendErrorStatus.FRIEND_CODE_NOT_FOUND));

        // 2. 내 코드를 입력한 경우
        if (targetUserId.equals(myUserId)) {
            throw codeInputFailure(myUserId, FriendErrorStatus.CANNOT_ADD_SELF);
        }

        // TODO: 차단(UserBlock) 기능이 들어오는 소셜 3/4에서, 어느 방향이든 차단 관계면
        //       차단 여부를 노출하지 않도록 FRIEND_CODE_NOT_FOUND(codeInputFailure)로 응답하는 검사를 이 자리에 추가한다.

        // 3. 이미 친구
        if (friendshipRepository.existsByUserLowIdAndUserHighId(
                Math.min(myUserId, targetUserId), Math.max(myUserId, targetUserId))) {
            throw new GeneralException(FriendErrorStatus.ALREADY_FRIENDS);
        }

        // 4. 상대가 나에게 이미 보낸 PENDING 요청이 있으면 새로 만들지 않고 그 요청을 자동 수락한다.
        Optional<FriendRequest> reverse = friendRequestRepository.findByRequesterIdAndReceiverId(targetUserId, myUserId);
        if (reverse.isPresent() && reverse.get().isPending()) {
            FriendRequest incoming = reverse.get();
            incoming.accept();
            try {
                friendshipRepository.saveAndFlush(Friendship.of(myUserId, targetUserId));
            } catch (DataIntegrityViolationException e) {
                // 같은 순간 다른 경로(상대의 수락 등)로 이미 친구가 된 경우 UNIQUE(low, high)에 걸린다.
                throw new GeneralException(FriendErrorStatus.ALREADY_FRIENDS);
            }
            return converter.toSendDto(incoming, true);
        }

        // 5. 내가 상대에게 보낸 요청이 이미 있는 경우
        Optional<FriendRequest> mine = friendRequestRepository.findByRequesterIdAndReceiverId(myUserId, targetUserId);
        if (mine.isPresent()) {
            FriendRequest existing = mine.get();
            if (existing.isPending()) {
                throw new GeneralException(FriendErrorStatus.REQUEST_ALREADY_SENT);
            }
            // 거절됐거나, 수락됐지만 이후 친구를 삭제한 경우: 새 행을 만들지 않고 기존 행을 되돌려 재요청한다.
            existing.reopen();
            return converter.toSendDto(existing, false);
        }

        // 6. 처음 보내는 요청
        try {
            FriendRequest saved = friendRequestRepository.saveAndFlush(FriendRequest.create(myUserId, targetUserId));
            return converter.toSendDto(saved, false);
        } catch (DataIntegrityViolationException e) {
            // 더블탭·동시 요청으로 UNIQUE(requester_id, receiver_id)에 걸린 경우 = 이미 요청함
            throw new GeneralException(FriendErrorStatus.REQUEST_ALREADY_SENT);
        }
    }

    @Transactional
    @Override
    public FriendRequestAcceptResponseDTO acceptFriendRequest(Long myUserId, Long requestId) {

        FriendRequest request = loadRespondableRequest(myUserId, requestId);
        Long requesterId = request.getRequesterId();

        // 수락 시점에 상대 상태를 다시 검사한다. 그 사이 탈퇴/휴면이 된 사용자의 요청은 없는 요청으로 취급한다.
        userRepository.findById(requesterId)
                .filter(user -> user.getStatus() == UserStatus.NORMAL)
                .orElseThrow(() -> new GeneralException(FriendErrorStatus.REQUEST_NOT_FOUND));

        // TODO: 소셜 3/4(차단)에서 수락 시점에 차단 관계를 다시 검사해, 차단됐다면 REQUEST_NOT_FOUND로 처리한다.

        request.accept();
        try {
            friendshipRepository.saveAndFlush(Friendship.of(myUserId, requesterId));
        } catch (DataIntegrityViolationException e) {
            // 이미 친구 관계가 만들어진 상태 = 다른 경로로 먼저 처리된 요청
            throw new GeneralException(FriendErrorStatus.REQUEST_ALREADY_HANDLED);
        }

        // 서로 동시에 요청해서 반대 방향 PENDING 요청이 남아 있을 수 있다. (양쪽 모두 요청 시점에는
        // 상대의 미커밋 요청을 볼 수 없다.) 이미 친구가 됐으므로 그 요청도 함께 수락 처리해 목록에 남지 않게 한다.
        friendRequestRepository.findByRequesterIdAndReceiverId(myUserId, requesterId)
                .filter(FriendRequest::isPending)
                .ifPresent(FriendRequest::accept);

        return converter.toAcceptDto(requesterId);
    }

    @Transactional
    @Override
    public void rejectFriendRequest(Long myUserId, Long requestId) {
        FriendRequest request = loadRespondableRequest(myUserId, requestId);
        request.reject();
    }

    @Transactional
    @Override
    public void deleteFriend(Long myUserId, Long friendUserId) {
        Friendship friendship = friendshipRepository
                .findByUserLowIdAndUserHighId(Math.min(myUserId, friendUserId), Math.max(myUserId, friendUserId))
                .orElseThrow(() -> new GeneralException(FriendErrorStatus.FRIENDSHIP_NOT_FOUND));

        friendshipRepository.delete(friendship);

        // TODO: 응원(Cheer) 기능이 들어오는 소셜 2/4에서, 둘 사이의 만료 전 응원도 이 트랜잭션에서 함께 삭제한다.
        // 기존 FriendRequest 행은 지우지 않는다. 다시 친구 요청을 하면 sendFriendRequest가 그 행을 PENDING으로 되돌린다.
    }

    // 수락/거절 공통 검증: 요청이 존재하고, 내가 받은 요청이며, 아직 처리되지 않았어야 한다.
    // 같은 요청을 동시에 수락·거절하는 경우를 막기 위해 행을 잠그고 조회한다.
    private FriendRequest loadRespondableRequest(Long myUserId, Long requestId) {
        FriendRequest request = friendRequestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new GeneralException(FriendErrorStatus.REQUEST_NOT_FOUND));

        if (!request.getReceiverId().equals(myUserId)) {
            throw new GeneralException(FriendErrorStatus.NOT_REQUEST_RECEIVER);
        }
        if (!request.isPending()) {
            throw new GeneralException(FriendErrorStatus.REQUEST_ALREADY_HANDLED);
        }
        return request;
    }

    // 코드 입력 실패(없는 코드 / 내 코드 / 차단 / 비활성 사용자)로 응답하면서 실패 횟수를 1회 기록한다.
    private GeneralException codeInputFailure(Long myUserId, FriendErrorStatus status) {
        attemptLimiter.recordFailure(myUserId);
        return new GeneralException(status);
    }
}
