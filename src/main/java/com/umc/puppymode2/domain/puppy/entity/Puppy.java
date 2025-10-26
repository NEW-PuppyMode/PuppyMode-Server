package com.umc.puppymode2.domain.puppy.entity;

import com.umc.puppymode2.domain.common.BaseEntity;
import com.umc.puppymode2.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@Setter
@Builder
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Puppy extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "puppy_level_id")
    private PuppyLevel puppyLevel;

    @Column(name = "puppy_name", nullable = false)
    private String puppyName;

    @Column(name = "puppy_exp", nullable = false)
    private Integer puppyExp;

    @Column(name = "is_custom_name", nullable = false)
    private boolean isCustomName = false;

    public void setPuppyName(String name) {
        this.puppyName = name;
        this.isCustomName = true;
    }
}
