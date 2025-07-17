package com.umc.puppymode2.domain.drinkhistory.repository;


import com.umc.puppymode2.domain.drinkhistory.entity.DrinkHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface DrinkHistoryRepository extends JpaRepository<DrinkHistory, Long> {
    boolean existsByUserIdAndDrinkDate(Long userId, LocalDate date);
}
