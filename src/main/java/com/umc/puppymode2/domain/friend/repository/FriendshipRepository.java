package com.umc.puppymode2.domain.friend.repository;

import com.umc.puppymode2.domain.friend.entity.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    // 호출하는 쪽에서 (low, high) 순서로 정규화해서 넘겨야 한다. (Friendship.of 참고)
    boolean existsByUserLowIdAndUserHighId(Long userLowId, Long userHighId);

    Optional<Friendship> findByUserLowIdAndUserHighId(Long userLowId, Long userHighId);

    // 내 친구들의 userId 목록. 친구 관계는 1행이라 내가 low/high 어느 쪽에 있든 상대 ID를 꺼낸다.
    @Query("SELECT CASE WHEN f.userLowId = :userId THEN f.userHighId ELSE f.userLowId END " +
            "FROM Friendship f " +
            "WHERE f.userLowId = :userId OR f.userHighId = :userId")
    List<Long> findFriendIds(@Param("userId") Long userId);

    // 친구 관계를 삭제하고 삭제된 행 수를 돌려준다. (0이면 이미 다른 트랜잭션이 지운 것)
    // 엔티티를 읽어 delete(entity)로 지우면, 읽은 뒤 다른 트랜잭션이 먼저 지웠을 때 0행 삭제가 예외(OptimisticLocking)로 번진다.
    // 호출하는 쪽에서 (low, high) 순서로 정규화해서 넘겨야 한다.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Friendship f WHERE f.userLowId = :userLowId AND f.userHighId = :userHighId")
    int deleteByPair(@Param("userLowId") Long userLowId, @Param("userHighId") Long userHighId);
}
