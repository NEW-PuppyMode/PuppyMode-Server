package com.umc.puppymode2.domain.block.repository;

import com.umc.puppymode2.domain.block.entity.UserBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {

    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    // 어느 쪽이 차단했든 두 사람 사이에 차단 관계가 있는지
    @Query("SELECT COUNT(b) > 0 FROM UserBlock b WHERE " +
            "(b.blockerId = :userA AND b.blockedId = :userB) OR (b.blockerId = :userB AND b.blockedId = :userA)")
    boolean existsBlockedBetween(@Param("userA") Long userA, @Param("userB") Long userB);

    // 나와 차단 관계에 있는 모든 사용자 ID. 내가 차단했든 나를 차단했든 상대 ID를 꺼낸다.
    @Query("SELECT CASE WHEN b.blockerId = :userId THEN b.blockedId ELSE b.blockerId END " +
            "FROM UserBlock b WHERE b.blockerId = :userId OR b.blockedId = :userId")
    List<Long> findRelatedUserIds(@Param("userId") Long userId);

    // 내가 차단한 목록 (최근 차단순). 같은 시각이면 ID가 큰 것을 먼저 보여준다.
    @Query("SELECT b FROM UserBlock b WHERE b.blockerId = :blockerId " +
            "ORDER BY b.createdAt DESC, b.userBlockId DESC")
    List<UserBlock> findAllByBlocker(@Param("blockerId") Long blockerId);

    // 차단 해제. 삭제된 행 수를 돌려준다. (0이면 차단한 기록이 없었던 것)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM UserBlock b WHERE b.blockerId = :blockerId AND b.blockedId = :blockedId")
    int deleteByBlockerAndBlocked(@Param("blockerId") Long blockerId, @Param("blockedId") Long blockedId);
}
