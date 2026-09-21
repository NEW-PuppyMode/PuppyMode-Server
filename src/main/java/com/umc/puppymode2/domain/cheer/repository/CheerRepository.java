package com.umc.puppymode2.domain.cheer.repository;

import com.umc.puppymode2.domain.cheer.entity.Cheer;
import com.umc.puppymode2.domain.user.entity.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface CheerRepository extends JpaRepository<Cheer, Long> {

    // 같은 (보낸 사람, 받는 사람, 대상 날짜)의 응원을 이미 보냈는지. UNIQUE 위반을 만나기 전에 빠르게 거르는 용도다.
    boolean existsBySenderIdAndReceiverIdAndTargetDate(Long senderId, Long receiverId, LocalDate targetDate);

    /*
     * 받은 응원 목록과 안 읽은 응원 수는 "보여줄 수 있는 응원"의 조건이 같아야 한다.
     * (목록에는 없는데 뱃지만 켜지거나 그 반대가 되지 않도록 두 쿼리의 WHERE를 똑같이 유지할 것)
     *   - 만료 전 (expires_at > now)
     *   - 보낸 사람이 지금도 나와 친구
     *   - 보낸 사람 상태가 NORMAL
     */

    // 받은 응원 목록: 받은 순서 최신순. 같은 시각이면 ID가 큰 것을 먼저 보여준다.
    @Query("SELECT c FROM Cheer c " +
            "WHERE c.receiverId = :receiverId AND c.expiresAt > :now " +
            "AND EXISTS (SELECT 1 FROM Friendship f WHERE " +
            "    (f.userLowId = c.senderId AND f.userHighId = c.receiverId) " +
            " OR (f.userLowId = c.receiverId AND f.userHighId = c.senderId)) " +
            "AND EXISTS (SELECT 1 FROM User u WHERE u.userId = c.senderId AND u.status = :normal) " +
            "ORDER BY c.createdAt DESC, c.cheerId DESC")
    List<Cheer> findReceivable(@Param("receiverId") Long receiverId,
                               @Param("now") LocalDateTime now,
                               @Param("normal") UserStatus normal);

    // 안 읽은 응원 수 (소셜 뱃지용)
    @Query("SELECT COUNT(c) FROM Cheer c " +
            "WHERE c.receiverId = :receiverId AND c.readAt IS NULL AND c.expiresAt > :now " +
            "AND EXISTS (SELECT 1 FROM Friendship f WHERE " +
            "    (f.userLowId = c.senderId AND f.userHighId = c.receiverId) " +
            " OR (f.userLowId = c.receiverId AND f.userHighId = c.senderId)) " +
            "AND EXISTS (SELECT 1 FROM User u WHERE u.userId = c.senderId AND u.status = :normal)")
    long countUnreadReceivable(@Param("receiverId") Long receiverId,
                               @Param("now") LocalDateTime now,
                               @Param("normal") UserStatus normal);

    /*
     * 내가 받은 안 읽은 만료 전 응원을 모두 읽음 처리한다. 이미 모두 읽은 상태여도 0건 갱신으로 정상 종료(멱등)
     *
     * 발신자가 NORMAL인 응원만 갱신한다. 목록/뱃지 쿼리는 NORMAL 발신자의 응원만 보여주므로, 여기서도 같은 조건을 써야
     * 발신자가 휴면(REST)인 동안 눈에 보이지 않는 응원이 읽음 처리되는 일이 없다.
     * (그렇게 읽음 처리되면 발신자가 NORMAL로 돌아왔을 때 목록에는 다시 나타나는데 뱃지는 이미 읽은 것으로 세어 어긋난다)
     * 친구 관계 조건은 넣지 않는다. 친구를 삭제하면 둘 사이의 응원이 함께 삭제되므로 친구가 아닌 사람의 응원은 남지 않는다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Cheer c SET c.readAt = :now, c.updatedAt = :now " +
            "WHERE c.receiverId = :receiverId AND c.readAt IS NULL AND c.expiresAt > :now " +
            "AND EXISTS (SELECT 1 FROM User u WHERE u.userId = c.senderId AND u.status = :normal)")
    int markAllAsRead(@Param("receiverId") Long receiverId,
                      @Param("now") LocalDateTime now,
                      @Param("normal") UserStatus normal);

    /*
     * 친구 목록의 「응원 보냄(SENT)」 판정용.
     * 내가 이 친구들에게 보낸 응원의 (친구, 대상 날짜)를 가져온다. 받은 사람 쪽 만료와 무관하게 행이 남아 있는 동안은
     * 계속 "보냄"이어야 하므로 expires_at 조건을 걸지 않는다. (만료 배치는 어제/오늘 건이 지워지지 않게 여유를 둔다)
     */
    @Query("SELECT c.receiverId AS receiverId, c.targetDate AS targetDate FROM Cheer c " +
            "WHERE c.senderId = :senderId AND c.receiverId IN :receiverIds AND c.targetDate IN :dates")
    List<CheerSentProjection> findSentTargets(@Param("senderId") Long senderId,
                                              @Param("receiverIds") Collection<Long> receiverIds,
                                              @Param("dates") Collection<LocalDate> dates);

    // 친구 삭제 시 둘 사이(양방향)의 응원을 모두 지운다. 이미 만료됐지만 아직 정리 배치가 안 지운 행도 함께 지워진다.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Cheer c WHERE " +
            "(c.senderId = :userA AND c.receiverId = :userB) OR (c.senderId = :userB AND c.receiverId = :userA)")
    int deleteAllBetween(@Param("userA") Long userA, @Param("userB") Long userB);

    // 만료 응원 정리 배치용: 기준 시각보다 전에 만료된 응원을 삭제한다.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Cheer c WHERE c.expiresAt < :cutoff")
    int deleteAllExpiredBefore(@Param("cutoff") LocalDateTime cutoff);
}
