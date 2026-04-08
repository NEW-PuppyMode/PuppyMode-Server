package com.umc.puppymode2.domain.puppy.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "level_exp")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LevelExp {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private int level;
    private int minExp;
    private int maxExp;
}
