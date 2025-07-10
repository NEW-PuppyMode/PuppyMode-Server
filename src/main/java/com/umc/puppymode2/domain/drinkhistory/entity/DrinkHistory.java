package com.umc.puppymode2.domain.drinkhistory.entity;

import com.umc.puppymode2.domain.common.BaseEntity;
import jakarta.persistence.Entity;
import lombok.*;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class DrinkHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long drinkHistoryId;

//    @ManyToOne
//    @JoinColumn(name = "user_id")
//    private User user;
    private Long userId;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean isDrink = true;

    @Column(nullable = false)
    private LocalDate drinkDate;


}
