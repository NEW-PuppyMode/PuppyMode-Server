package com.umc.puppymode2.domain.puppy.repository;

import com.umc.puppymode2.domain.puppy.entity.PuppyLevel;
import com.umc.puppymode2.domain.puppy.entity.PuppyType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PuppyLevelRepository extends JpaRepository<PuppyLevel, Long> {

    Optional<PuppyLevel> findByPuppyTypeAndPuppyLevel(PuppyType puppyType, int level);
}
