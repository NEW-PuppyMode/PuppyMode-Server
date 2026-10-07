package com.umc.puppymode2.domain.friend.repository;

import com.umc.puppymode2.domain.friend.entity.FriendRequest;
import com.umc.puppymode2.domain.friend.entity.enums.FriendRequestStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {

    // (보낸 사람, 받은 사람) 쌍의 요청 조회. UNIQUE(requester_id, receiver_id)라 최대 1건.
    Optional<FriendRequest> findByRequesterIdAndReceiverId(Long requesterId, Long receiverId);

    // 수락/거절 시 같은 요청을 동시에 처리하지 못하도록 행을 잠그고 조회한다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM FriendRequest r WHERE r.friendRequestId = :requestId")
    Optional<FriendRequest> findByIdForUpdate(@Param("requestId") Long requestId);

    // 내가 받은 요청 목록 (최신순). 같은 시각이면 ID가 큰 것을 먼저 보여준다.
    @Query("SELECT r FROM FriendRequest r " +
            "WHERE r.receiverId = :receiverId AND r.status = :status " +
            "ORDER BY r.createdAt DESC, r.friendRequestId DESC")
    List<FriendRequest> findAllByReceiverAndStatus(@Param("receiverId") Long receiverId,
                                                   @Param("status") FriendRequestStatus status);

    // 차단 시 둘 사이(양방향)의 대기 중 요청을 지운다. 이미 수락·거절된 요청은 이력이라 그대로 둔다.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM FriendRequest r WHERE r.status = com.umc.puppymode2.domain.friend.entity.enums.FriendRequestStatus.PENDING AND " +
            "((r.requesterId = :userA AND r.receiverId = :userB) OR (r.requesterId = :userB AND r.receiverId = :userA))")
    int deletePendingBetween(@Param("userA") Long userA, @Param("userB") Long userB);
}
