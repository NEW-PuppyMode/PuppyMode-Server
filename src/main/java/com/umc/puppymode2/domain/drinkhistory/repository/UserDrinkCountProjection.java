package com.umc.puppymode2.domain.drinkhistory.repository;

public interface UserDrinkCountProjection {
    Long getUserId();
    Long getTotalCount();
    Long getDrinkCount();
}
