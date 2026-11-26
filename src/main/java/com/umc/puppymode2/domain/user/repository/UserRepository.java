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

    // TODO(#228): 임시 대시보드용. Amplitude 연동 후 제거
    // 레벨별 유저 수
    // 강아지가 없는 유저(온보딩 전)는 level = NULL로 집계
    @Query(value = """
            SELECT l.level AS level,
                   COUNT(*) AS count
            FROM `user` u
            LEFT JOIN puppy p ON p.user_id = u.user_id
            LEFT JOIN level_exp l
                   ON p.puppy_exp >= l.min_exp
                  AND (p.puppy_exp < l.max_exp
                       OR l.level = (SELECT MAX(level) FROM level_exp))
            WHERE u.status = 'NORMAL'
            GROUP BY l.level
            """, nativeQuery = true)
    List<LevelCount> countByLevel();

    // TODO(#228): 임시 대시보드용.
    interface LevelCount {
        Integer getLevel();
        long getCount();
    }
}
