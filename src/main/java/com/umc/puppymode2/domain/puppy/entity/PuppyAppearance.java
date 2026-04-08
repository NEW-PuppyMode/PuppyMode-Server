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
    private PuppyType puppyType;
    private int stage;
    private int levelStart;
    private int levelEnd;
    private String stageName;
    private String imageUrl;
}
