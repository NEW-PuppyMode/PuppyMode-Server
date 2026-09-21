package com.umc.puppymode2.domain.friend.entity;

import com.umc.puppymode2.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자별 친구 코드.
 *
 * 설계서는 User 테이블에 friend_code 컬럼을 추가하는 안이지만, 다른 도메인(user)의 엔티티를
 * 수정하지 않기 위해 friend 도메인의 별도 테이블로 둔다. (사용자 1명당 코드 1개)
 *
 * - user_id UNIQUE : 한 사용자에게 코드가 두 개 발급되는 것을 막는다. (동시에 최초 조회한 경우의 최종 방어선)
 * - code UNIQUE    : 서로 다른 사용자가 같은 코드를 갖는 것을 막는다. (충돌 시 발급 로직이 재시도)
 */
@Entity
@Table(
        name = "friend_code",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_friend_code_user", columnNames = {"user_id"}),
                @UniqueConstraint(name = "uk_friend_code_code", columnNames = {"code"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FriendCode extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "friend_code_id")
    private Long friendCodeId;

    // 다른 소셜 엔티티들과 마찬가지로 User 연관관계 대신 ID만 보관한다. (goal 도메인과 동일한 방식)
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // 현재는 4자리 숫자로 시작하고, 포화되면 최대 8자리까지 자릿수가 늘어난다. (가변 길이라 문자열로 저장)
    @Column(name = "code", nullable = false, length = 8)
    private String code;

    public static FriendCode of(Long userId, String code) {
        FriendCode friendCode = new FriendCode();
        friendCode.userId = userId;
        friendCode.code = code;
        return friendCode;
    }
}
