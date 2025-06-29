package com.umc.puppymode2.domain.temp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.umc.puppymode2.domain.temp.entity.Temp;
import com.umc.puppymode2.domain.temp.entity.enums.TempStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface TempRepository extends JpaRepository<Temp, Long> {

    List<Temp> findByStatus(TempStatus status);

    Optional<Temp> findByIdAndStatus(Long id, TempStatus status);

    List<Temp> findByNameContaining(String name);
}