package com.umc.puppymode2.domain.drinkhistory.repository;


import com.umc.puppymode2.domain.drinkhistory.entity.DrinkHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DrinkHistoryRepository extends JpaRepository<DrinkHistory, Long> {
    boolean existsByUserUserIdAndDrinkDate(Long userId, LocalDate date);
    List<DrinkHistory> findAllByUserUserIdAndDrinkDateBetween(Long userId, LocalDate start, LocalDate end);
    long countByUserUserIdAndDrinkDateBetween(Long userId, LocalDate start, LocalDate end);
    @Query("SELECT COUNT(DISTINCT d.drinkDate) FROM DrinkHistory d WHERE d.user.userId = :userId AND d.drinkDate BETWEEN :start AND :end")
    long countDistinctDrinkDates(@Param("userId") Long userId,
                                 @Param("start") LocalDate start,
                                 @Param("end") LocalDate end);
    long countByUserUserIdAndIsDrinkTrueAndDrinkDateBetween(Long userId, LocalDate start, LocalDate end);
    long countByUserUserIdAndIsDrinkTrue(Long userId);

    // 유저별 이번 달 전체 음주 기록 수 + 음주 횟수
    @Query("""
        SELECT d.user.userId AS userId,
               COUNT(d) AS totalCount,
               SUM(CASE WHEN d.isDrink = true THEN 1 ELSE 0 END) AS drinkCount
        FROM DrinkHistory d
        WHERE d.user.userId IN :userIds
          AND d.drinkDate BETWEEN :firstDay AND :lastDay
        GROUP BY d.user.userId
    """)
    List<UserDrinkCountProjection> countMonthlyDrinkByUserIds(
            @Param("userIds") List<Long> userIds,
            @Param("firstDay") LocalDate firstDay,
            @Param("lastDay") LocalDate lastDay
    );
}
