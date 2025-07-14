package com.umc.puppymode2.domain.onboarding.entity;

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
// 관련된 엔티티/enum/repository 가 미리 생성되지 않아 임시로 구현한 파일입니다.
// 이후 다른 분들 코드도 머지되면, 해당 코드 속 파일로 import 변경하도록 하겠습니다!
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}
