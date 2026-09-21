package com.umc.puppymode2.domain.cheer.repository;

import com.umc.puppymode2.domain.friend.entity.Friendship;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * 응원 보내기에서 친구 관계를 "잠그고" 확인하기 위한 Repository.
 *
 * friend 도메인의 FriendshipRepository에는 손대지 않고, cheer 도메인 안에서 같은 엔티티를 잠금 조회한다.
 *
 * <h4>왜 잠그는가</h4>
 * 응원 발송은 (1) 친구 관계 확인 (2) 응원 INSERT 두 단계인데, 그 사이에 친구 삭제가 끼어들면
 * 친구 관계와 그 시점의 응원만 지워지고 뒤늦게 (2)가 실행되어 응원 행이 남을 수 있다.
 * 남은 응원은 두 사람이 나중에 다시 친구가 되는 순간 받은 응원 목록에 다시 나타난다.
 *
 * 공유 잠금(FOR SHARE)으로 조회하면 InnoDB에서 다음처럼 직렬화된다.
 *  - 발송이 먼저: 발송 트랜잭션이 끝날 때까지 친구 삭제(DELETE)가 기다린다. 삭제는 그 뒤에 방금 저장된 응원까지 함께 지운다.
 *  - 삭제가 먼저: 삭제가 끝날 때까지 발송의 조회가 기다리고, 끝난 뒤에는 행이 없으므로 "친구 아님"으로 처리된다.
 * 발송끼리는 공유 잠금이라 서로 기다리지 않는다.
 */
public interface CheerFriendshipRepository extends Repository<Friendship, Long> {

    // 호출하는 쪽에서 (low, high) 순서로 정규화해서 넘겨야 한다. 반드시 트랜잭션 안에서 호출할 것 (잠금은 트랜잭션이 끝날 때 풀린다)
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("SELECT f FROM Friendship f WHERE f.userLowId = :userLowId AND f.userHighId = :userHighId")
    Optional<Friendship> findWithSharedLock(@Param("userLowId") Long userLowId,
                                            @Param("userHighId") Long userHighId);
}
