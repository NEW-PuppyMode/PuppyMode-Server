package com.umc.puppymode2.domain.goal.repository;

import com.umc.puppymode2.domain.goal.entity.UserGoalHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
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

    // 온보딩 최초 목표 설정 완료 여부 판단용 (월과 무관하게 이력이 하나라도 있는지)
    boolean existsByUserId(Long userId);

    @Modifying
    @Query("DELETE FROM UserGoalHistory ugh WHERE ugh.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    // 이번 달 보상 미지급 목록 조회
    List<UserGoalHistory> findAllByGoalMonthAndRewardedFalse(LocalDate goalMonth);

    // 해당 유저가 목표를 설정한 적 있는 모든 월(goalMonth)을 오름차순으로 조회한다.
    // (user_id, goal_month) UNIQUE 제약이 있어 월별로 row가 최대 1개이므로 DISTINCT는 불필요하다.
    // 캘린더 모달에서 "목표가 설정된 월"만 활성화하는 데 사용한다.
    @Query("SELECT ugh.goalMonth FROM UserGoalHistory ugh WHERE ugh.userId = :userId ORDER BY ugh.goalMonth")
    List<LocalDate> findGoalMonthsByUserId(@Param("userId") Long userId);

}