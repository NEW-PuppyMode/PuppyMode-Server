package com.umc.puppymode2.domain.notification.repository;

import com.umc.puppymode2.domain.notification.dto.DrinkReminderTarget;
import com.umc.puppymode2.domain.notification.entity.FcmToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {

    Optional<FcmToken> findByFcmToken(String fcmToken);

    void deleteByFcmToken(String fcmToken);

    void deleteByFcmTokenAndUserUserId(String fcmToken, Long userId);

    @Modifying
    @Query("DELETE FROM FcmToken ft WHERE ft.user.userId = :userId")
    void deleteAllByUserUserId(@Param("userId") Long userId);

    @Query("""
        SELECT new com.umc.puppymode2.domain.notification.dto.DrinkReminderTarget(ft.fcmToken, u.username)
        FROM FcmToken ft
        JOIN ft.user u
        WHERE u.receiveNotifications = true
          AND u.status = 'NORMAL'
          AND NOT EXISTS (
              SELECT 1 FROM DrinkHistory dh
              WHERE dh.user = u
                AND dh.drinkDate = :today
          )
        """)
    List<DrinkReminderTarget> findTargetsForDrinkReminder(@Param("today") LocalDate today);

    List<FcmToken> findByUserUserId(Long userId);
}