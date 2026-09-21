package com.umc.puppymode2.domain.social.repository;

import com.umc.puppymode2.domain.friend.entity.FriendRequest;
import com.umc.puppymode2.domain.friend.entity.enums.FriendRequestStatus;
import com.umc.puppymode2.domain.user.entity.enums.UserStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * 소셜 뱃지용 집계 Repository.
 *
 * 받은 친구 요청 수는 friend 도메인의 Repository에 메서드를 추가하지 않고, social 도메인에서 같은 엔티티를
 * 읽기 전용으로 집계한다.
 */
public interface SocialSummaryRepository extends Repository<FriendRequest, Long> {

    /*
     * 받은 친구 요청 목록(GET /friend-requests/received)에 실제로 보이는 요청과 같은 조건으로 센다.
     * (목록에는 없는데 뱃지만 켜지는 일이 없도록 조건을 맞춘다)
     *   - 내가 받은 PENDING 요청
     *   - 요청자가 NORMAL (탈퇴/휴면 사용자 제외)
     *   - 요청자와 아직 친구가 아님 (서로 동시에 요청해 이미 친구가 됐는데 PENDING이 남은 경우 제외)
     */
    @Query("SELECT COUNT(r) FROM FriendRequest r " +
            "WHERE r.receiverId = :receiverId AND r.status = :status " +
            "AND EXISTS (SELECT 1 FROM User u WHERE u.userId = r.requesterId AND u.status = :normal) " +
            "AND NOT EXISTS (SELECT 1 FROM Friendship f WHERE " +
            "    (f.userLowId = r.requesterId AND f.userHighId = r.receiverId) " +
            " OR (f.userLowId = r.receiverId AND f.userHighId = r.requesterId))")
    long countVisibleReceivedRequests(@Param("receiverId") Long receiverId,
                                      @Param("status") FriendRequestStatus status,
                                      @Param("normal") UserStatus normal);
}
