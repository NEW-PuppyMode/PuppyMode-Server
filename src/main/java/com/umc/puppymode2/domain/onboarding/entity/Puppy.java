package com.umc.puppymode2.domain.onboarding.entity;

import com.umc.puppymode2.domain.common.BaseEntity;
import com.umc.puppymode2.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@Builder
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
// 관련된 엔티티/enum/repository 가 미리 생성되지 않아 임시로 구현한 파일입니다.
// 이후 다른 분들 코드도 머지되면, 해당 코드 속 파일로 import 변경하도록 하겠습니다!
public class Puppy extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "user_id")
//    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "puppy_level_id")
    private PuppyLevel puppyLevel;

    @Column(name = "puppy_name", nullable = false)
    private String puppyName;

    @Column(name = "puppy_exp", nullable = false)
    private Integer puppyExp;
}
