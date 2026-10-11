package com.umc.puppymode2.domain.user.repository;

import com.umc.puppymode2.domain.user.entity.User;
import com.umc.puppymode2.domain.user.entity.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    // TODO(#213): 임시 대시보드용. Amplitude 연동 후 제거
    long countByStatus(UserStatus status);

    // TODO(#213): 임시 대시보드용. Amplitude 연동 후 제거
    // 날짜별 가입 수 (created_at은 UTC 저장이므로 KST로 보정)
    @Query(value = """
            SELECT DATE_FORMAT(created_at + INTERVAL 9 HOUR, '%Y-%m-%d') AS date,
                   COUNT(*) AS count
            FROM `user`
            WHERE status = 'NORMAL'
            GROUP BY date
            ORDER BY date DESC
            """, nativeQuery = true)
    List<DailySignup> countDailySignups();

    // TODO(#213): 임시 대시보드용. Amplitude 연동 후 제거
    interface DailySignup {
        String getDate();
        long getCount();
    }
}
