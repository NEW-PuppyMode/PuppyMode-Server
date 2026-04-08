package com.umc.puppymode2.domain.puppy.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "puppy_appearance")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PuppyAppearance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "puppy_type", nullable = false)
    private PuppyType puppyType;

    @Column(name = "stage", nullable = false)
    private int stage;

    @Column(name = "level_start", nullable = false)
    private int levelStart;

    @Column(name = "level_end", nullable = false)
    private int levelEnd;

    @Column(name = "stage_name", nullable = false)
    private String stageName;

    @Column(name = "image_url")
    private String imageUrl;
}
