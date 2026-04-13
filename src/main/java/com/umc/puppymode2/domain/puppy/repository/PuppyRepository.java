package com.umc.puppymode2.domain.puppy.repository;

import com.umc.puppymode2.domain.puppy.entity.Puppy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PuppyRepository extends JpaRepository<Puppy, Long> {
    Optional<Puppy> findByUser_UserId(Long userId);

    boolean existsByUser_UserId(Long userId);
}
