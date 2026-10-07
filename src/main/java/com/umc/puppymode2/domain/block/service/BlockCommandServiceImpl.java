package com.umc.puppymode2.domain.block.service;

import com.umc.puppymode2.domain.block.dto.BlockCreateResponseDTO;
import com.umc.puppymode2.domain.block.entity.UserBlock;
import com.umc.puppymode2.domain.block.exception.BlockErrorStatus;
import com.umc.puppymode2.domain.block.repository.UserBlockRepository;
import com.umc.puppymode2.domain.cheer.repository.CheerRepository;
import com.umc.puppymode2.domain.friend.converter.FriendConverter;
import com.umc.puppymode2.domain.friend.repository.FriendRequestRepository;
import com.umc.puppymode2.domain.friend.repository.FriendshipRepository;
import com.umc.puppymode2.domain.user.entity.enums.UserStatus;
import com.umc.puppymode2.domain.user.repository.UserRepository;
import com.umc.puppymode2.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BlockCommandServiceImpl implements BlockCommandService {

    private final UserBlockRepository userBlockRepository;
    private final FriendshipRepository friendshipRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final CheerRepository cheerRepository;
    private final UserRepository userRepository;
    private final FriendConverter friendConverter;

    @Transactional
    @Override
    public BlockCreateResponseDTO block(Long myUserId, Long targetUserId) {

        // 1. 자기 자신은 차단할 수 없다.
        if (myUserId.equals(targetUserId)) {
            throw new GeneralException(BlockErrorStatus.CANNOT_BLOCK_SELF);
        }

        // 2. 친구가 아니거나 NORMAL이 아닌 사용자는 같은 응답으로 처리한다. (응원 보내기의 NOT_FRIENDS와 같은 방식)
        userRepository.findById(targetUserId)
                .filter(user -> user.getStatus() == UserStatus.NORMAL)
                .orElseThrow(() -> new GeneralException(BlockErrorStatus.NOT_FRIENDS));

        // 3. 이미 차단했다면 409. 차단하면 친구 관계가 사라지므로, 친구 확인보다 먼저 검사해야 409에 도달한다.
        if (userBlockRepository.existsByBlockerIdAndBlockedId(myUserId, targetUserId)) {
            throw new GeneralException(BlockErrorStatus.ALREADY_BLOCKED);
        }

        // 4. 차단 기록을 가장 먼저 저장한다. 같은 요청이 동시에 두 번 들어오면 UNIQUE(blocker_id, blocked_id)가
        //    직렬화해서 한쪽은 여기서 409로 정리되므로, 아래 삭제 단계가 중복 실행되지 않는다.
        UserBlock saved;
        try {
            saved = userBlockRepository.saveAndFlush(UserBlock.of(myUserId, targetUserId));
        } catch (DataIntegrityViolationException e) {
            throw new GeneralException(BlockErrorStatus.ALREADY_BLOCKED);
        }

        // 5. 친구 관계를 삭제한다. 읽어서 지우지 않고 영향 행 수로 판단한다.
        //    - 삭제된 행이 없다면(친구가 아니거나, 그 사이 상대가 친구를 지웠거나, 서로 동시에 차단해 상대가 먼저 지운 경우)
        //      친구가 아닌 것이므로 예외로 4번의 차단 기록까지 롤백한다. (친구가 아닌데 차단 기록만 남는 상태를 막는다)
        //    - 엔티티를 읽은 뒤 delete(entity)로 지우면 동시에 지워진 경우 0행 삭제가 OptimisticLocking 예외(500)로 번진다.
        int deletedFriendships = friendshipRepository.deleteByPair(
                Math.min(myUserId, targetUserId), Math.max(myUserId, targetUserId));
        if (deletedFriendships == 0) {
            throw new GeneralException(BlockErrorStatus.NOT_FRIENDS);
        }

        // 6. 둘 사이의 응원(만료 여부와 무관)과 대기 중 친구 요청(양방향)을 같은 트랜잭션에서 함께 지운다.
        //    응원 보내기는 친구 관계 행을 공유 잠금으로 읽으므로, 동시에 보낸 응원도 5번의 삭제를 기다린 뒤 여기서 함께 지워진다.
        cheerRepository.deleteAllBetween(myUserId, targetUserId);
        friendRequestRepository.deletePendingBetween(myUserId, targetUserId);

        return BlockCreateResponseDTO.builder()
                .userId(targetUserId)
                .blockedAt(friendConverter.toKstOffset(saved.getCreatedAt()))
                .build();
    }

    @Transactional
    @Override
    public void unblock(Long myUserId, Long targetUserId) {
        // 친구 관계는 복구하지 않는다. 해제 후에는 친구 요청을 다시 보내야 한다.
        int deleted = userBlockRepository.deleteByBlockerAndBlocked(myUserId, targetUserId);
        if (deleted == 0) {
            throw new GeneralException(BlockErrorStatus.BLOCK_NOT_FOUND);
        }
    }
}
