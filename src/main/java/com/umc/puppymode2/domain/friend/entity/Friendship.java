package com.umc.puppymode2.domain.friend.entity;

import com.umc.puppymode2.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 친구 관계.
 *
 * 친구 관계는 대칭이므로 두 사용자당 1행만 저장한다. 항상 user_low_id < user_high_id 가 되도록
 * 정규화해서 넣기 때문에 (A,B)와 (B,A)가 중복 저장될 수 없고, 삭제도 한 번에 끝난다.
 * 정규화는 반드시 {@link #of(Long, Long)}를 통해서만 한다.
 */
@Entity
@Table(
        name = "friendship",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_friendship_pair", columnNames = {"user_low_id", "user_high_id"})
        },
        indexes = {
                // (low, high) UNIQUE 인덱스는 low 조회에 쓰이므로, high 쪽 조회를 위한 인덱스를 따로 둔다.
                @Index(name = "idx_friendship_high", columnList = "user_high_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Friendship extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "friendship_id")
    private Long friendshipId;

    // 두 사용자 중 ID가 작은 쪽
    @Column(name = "user_low_id", nullable = false)
    private Long userLowId;

    // 두 사용자 중 ID가 큰 쪽
    @Column(name = "user_high_id", nullable = false)
    private Long userHighId;

    public static Friendship of(Long userA, Long userB) {
        if (userA.equals(userB)) {
            throw new IllegalArgumentException("자기 자신과는 친구가 될 수 없습니다.");
        }
        Friendship friendship = new Friendship();
        friendship.userLowId = Math.min(userA, userB);
        friendship.userHighId = Math.max(userA, userB);
        return friendship;
    }
}
