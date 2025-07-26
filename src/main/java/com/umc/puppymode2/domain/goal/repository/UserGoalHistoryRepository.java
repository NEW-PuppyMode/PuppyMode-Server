package com.umc.puppymode2.domain.goal.repository;

import com.umc.puppymode2.domain.goal.entity.UserGoalHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface UserGoalHistoryRepository extends JpaRepository<UserGoalHistory, Long> {
    Optional<UserGoalHistory> findTopByUserIdOrderByGoalSetAtDesc(Long userId);
}