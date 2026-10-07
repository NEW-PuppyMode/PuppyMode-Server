package com.umc.puppymode2.domain.block.entity;

import com.umc.puppymode2.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자 차단.
 *
 * 차단은 방향이 있으므로 (blocker_id, blocked_id) 쌍당 행은 최대 1개다. (Friendship처럼 정규화하지 않는다)
 * 친구만 차단할 수 있고, 차단하면 친구 관계·둘 사이의 응원·대기 중 친구 요청이 함께 삭제된다.
 * 차단 관계 검사는 항상 어느 쪽이 차단했든 양방향으로 한다.
 */
@Entity
@Table(
        name = "user_block",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_block_pair", columnNames = {"blocker_id", "blocked_id"})
        },
        indexes = {
                // (blocker, blocked) UNIQUE 인덱스는 blocker 조회에 쓰이므로, blocked 쪽 조회(나를 차단한 사람)를 위한 인덱스를 따로 둔다.
                @Index(name = "idx_user_block_blocked", columnList = "blocked_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserBlock extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_block_id")
    private Long userBlockId;

    // 차단한 사용자
    @Column(name = "blocker_id", nullable = false)
    private Long blockerId;

    // 차단당한 사용자
    @Column(name = "blocked_id", nullable = false)
    private Long blockedId;

    public static UserBlock of(Long blockerId, Long blockedId) {
        if (blockerId.equals(blockedId)) {
            throw new IllegalArgumentException("자기 자신은 차단할 수 없습니다.");
        }
        UserBlock block = new UserBlock();
        block.blockerId = blockerId;
        block.blockedId = blockedId;
        return block;
    }
}
