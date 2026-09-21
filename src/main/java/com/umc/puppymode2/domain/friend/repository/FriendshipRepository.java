package com.umc.puppymode2.domain.friend.repository;

import com.umc.puppymode2.domain.friend.entity.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
