package com.umc.puppymode2.domain.puppy.repository;

import com.umc.puppymode2.domain.puppy.entity.PuppyAppearance;
import com.umc.puppymode2.domain.puppy.entity.PuppyType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PuppyAppearanceRepository extends JpaRepository<PuppyAppearance, Long> {

    @Query("SELECT a FROM PuppyAppearance a " +
            "WHERE a.puppyType = :type " +
            "AND a.levelStart <= :level AND a.levelEnd >= :level")
    Optional<PuppyAppearance> findByPuppyTypeAndLevel(
            @Param("type") PuppyType puppyType,
            @Param("level") int level
    );
}
