package com.umc.puppymode2.domain.friend.repository;

import com.umc.puppymode2.domain.drinkhistory.entity.DrinkHistory;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/**
 * 친구 목록용 음주 기록 조회 전용 Repository.
 *
 * DrinkHistory 엔티티의 기존 Repository(drinkhistory 도메인)에는 손대지 않고,
 * friend 도메인 안에서 같은 엔티티를 읽기 전용으로 조회하기 위해 별도로 둔다.
 * (친구 N명 x 어제/오늘을 쿼리 1번으로 가져오는 것이 목적)
 */
public interface FriendDrinkRecordRepository extends Repository<DrinkHistory, Long> {

    // is_drink = false 인 행도 함께 가져온다. (NOT_DRANK: "오늘 기록은 있는데 마시지 않음" 판정에 필요)
    @Query("SELECT d.user.userId AS userId, d.drinkDate AS drinkDate, d.isDrink AS isDrink " +
            "FROM DrinkHistory d " +
            "WHERE d.user.userId IN :userIds AND d.drinkDate IN :dates")
    List<FriendDrinkRecordProjection> findRecords(@Param("userIds") Collection<Long> userIds,
                                                  @Param("dates") Collection<LocalDate> dates);
}
