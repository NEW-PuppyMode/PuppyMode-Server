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

    @Column(name = "level", nullable = false)
    private int level;

    @Column(name = "min_exp", nullable = false)
    private int minExp;

    @Column(name = "max_exp", nullable = false)
    private int maxExp;
}
