package com.umc.puppymode2.domain.puppy.entity;

import com.umc.puppymode2.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@Builder
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "puppy_level", uniqueConstraints = @UniqueConstraint(columnNames = {"puppy_type", "puppy_level"}))
public class PuppyLevel extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "puppy_type", nullable = false)
    private PuppyType puppyType;

    @Column(name = "puppy_level", nullable = false)
    private Integer puppyLevel;

    @Column(name = "level_name", nullable = false)
    private String levelName;

    @Column(name = "level_image_url")
    private String levelImageUrl;

    @Column(name = "level_min_exp", nullable = false)
    private Long levelMinExp;

    @Column(name = "level_max_exp", nullable = false)
    private Long levelMaxExp;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "next_level_id")
    private PuppyLevel nextLevel;
}
