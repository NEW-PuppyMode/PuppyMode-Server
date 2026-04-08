package com.umc.puppymode2.domain.puppy.repository;

import com.umc.puppymode2.domain.puppy.entity.LevelExp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LevelExpRepository extends JpaRepository<LevelExp, Long> {

    @Query("SELECT l FROM LevelExp l " +
            "WHERE l.minExp <= :exp AND :exp < l.maxExp")
    Optional<LevelExp> findByExp(@Param("exp") int exp);
}
