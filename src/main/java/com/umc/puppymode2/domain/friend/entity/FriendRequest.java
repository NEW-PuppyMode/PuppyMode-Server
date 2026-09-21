package com.umc.puppymode2.domain.friend.entity;

import com.umc.puppymode2.domain.common.BaseEntity;
import com.umc.puppymode2.domain.friend.entity.enums.FriendRequestStatus;
import com.umc.puppymode2.global.util.TimeConstants;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 친구 요청.
 *
 * (requester_id, receiver_id) 쌍당 행은 최대 1개다. 거절(REJECTED)된 뒤 다시 요청하거나,
 * 친구 삭제 후 다시 요청하는 경우에도 새 행을 만들지 않고 기존 행을 PENDING으로 되돌린다.
 * 그래서 UNIQUE 하나만으로 중복 요청(더블탭, 동시 요청)을 최종적으로 막을 수 있다.
 */
@Entity
@Table(
        name = "friend_request",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_friend_request_pair", columnNames = {"requester_id", "receiver_id"})
        },
        indexes = {
                // 받은 요청 목록 / 소셜 뱃지(받은 PENDING 수) 조회용
                @Index(name = "idx_friend_request_receiver", columnList = "receiver_id, status")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FriendRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "friend_request_id")
    private Long friendRequestId;

    // 요청을 보낸 사용자
    @Column(name = "requester_id", nullable = false)
    private Long requesterId;

    // 요청을 받은 사용자
    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private FriendRequestStatus status;

    // 수락/거절한 시각. PENDING이면 null
    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    public static FriendRequest create(Long requesterId, Long receiverId) {
        if (requesterId.equals(receiverId)) {
            throw new IllegalArgumentException("자기 자신에게는 친구 요청을 보낼 수 없습니다.");
        }
        FriendRequest request = new FriendRequest();
        request.requesterId = requesterId;
        request.receiverId = receiverId;
        request.status = FriendRequestStatus.PENDING;
        return request;
    }

    public boolean isPending() {
        return status == FriendRequestStatus.PENDING;
    }

    public void accept() {
        this.status = FriendRequestStatus.ACCEPTED;
        this.respondedAt = LocalDateTime.now(TimeConstants.KST);
    }

    public void reject() {
        this.status = FriendRequestStatus.REJECTED;
        this.respondedAt = LocalDateTime.now(TimeConstants.KST);
    }

    // 거절됐거나(친구 삭제 후의) 수락 처리된 요청을 다시 대기 상태로 되돌린다. (재요청)
    public void reopen() {
        this.status = FriendRequestStatus.PENDING;
        this.respondedAt = null;
    }
}
