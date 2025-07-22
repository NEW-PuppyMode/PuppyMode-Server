package com.umc.puppymode2.domain.drinkhistory.repository;


import com.umc.puppymode2.domain.drinkhistory.entity.DrinkHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DrinkHistoryRepository extends JpaRepository<DrinkHistory, Long> {
    boolean existsByUserUserIdAndDrinkDate(Long userId, LocalDate date);
    List<DrinkHistory> findAllByUserUserIdAndDrinkDateBetween(Long userId, LocalDate start, LocalDate end);
}
