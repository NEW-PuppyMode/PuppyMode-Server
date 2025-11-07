package com.umc.puppymode2.domain.user.repository;

import com.umc.puppymode2.domain.user.entity.WithdrawnUserArchive;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface WithDrawnUserArchiveRepository extends JpaRepository<WithdrawnUserArchive, Long> {

    @Modifying
    @Query("DELETE FROM WithdrawnUserArchive w WHERE w.dataRetentionUntil < :now")
    void deleteExpiredArchive(@Param("now") LocalDateTime now);
}
