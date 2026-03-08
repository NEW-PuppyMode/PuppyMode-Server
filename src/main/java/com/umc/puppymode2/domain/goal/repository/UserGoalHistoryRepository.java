package com.umc.puppymode2.domain.goal.repository;

import com.umc.puppymode2.domain.goal.entity.UserGoalHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.Optional;
import java.time.LocalDate;

@Repository
public interface UserGoalHistoryRepository extends JpaRepository<UserGoalHistory, Long> {
    Optional<UserGoalHistory> findTopByUserIdOrderByGoalSetAtDesc(Long userId);
    Optional<UserGoalHistory> findTopByUserIdAndGoalSetAtLessThanEqualOrderByGoalSetAtDesc(
            Long userId, LocalDateTime asOfDate
    );

    // 특정 월 목표 조회
    Optional<UserGoalHistory> findByUserIdAndGoalMonth(Long userId, LocalDate goalMonth);

    // 특정 월 목표 존재 여부 확인 (중복 요청 방지)
    boolean existsByUserIdAndGoalMonth(Long userId, LocalDate goalMonth);

    // 가장 최근 목표 조회 (기존 목표 유지 기능)
    Optional<UserGoalHistory> findTopByUserIdOrderByGoalMonthDesc(Long userId);


    @Modifying
    @Query("DELETE FROM UserGoalHistory ugh WHERE ugh.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}