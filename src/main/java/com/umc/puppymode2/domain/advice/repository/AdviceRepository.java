package com.umc.puppymode2.domain.advice.repository;

import com.umc.puppymode2.domain.advice.entity.Advice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface AdviceRepository extends JpaRepository<Advice, Long> {
     // 특정 사용자가 특정 기간 동안 들은 조언(한마디) 횟수를 카운트합니다.
    long countByUserUserIdAndAdvisedAtBetween(Long userId, LocalDateTime startDate, LocalDateTime endDate);
}
