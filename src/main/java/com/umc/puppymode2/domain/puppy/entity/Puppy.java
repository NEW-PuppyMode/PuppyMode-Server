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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "puppy_type", nullable = false)
    private PuppyType puppyType;

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
