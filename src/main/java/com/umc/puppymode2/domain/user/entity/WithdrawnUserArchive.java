package com.umc.puppymode2.domain.user.entity;

import com.umc.puppymode2.domain.common.BaseEntity;
import com.umc.puppymode2.domain.user.auth.enums.Provider;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "withdrawn_user_archive")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class WithdrawnUserArchive extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long archiveId;

    private Long originalUserId;
    private String maskedEmail;

    @Enumerated(EnumType.STRING)
    private Provider provider;

    private LocalDateTime withdrawnAt;
    private LocalDateTime signupAt;
    private Integer totalDrinkDays;
    private LocalDateTime dataRetentionUntil;

    public static WithdrawnUserArchive fromUser(User user, Integer totalDrinkDays) {
        return WithdrawnUserArchive.builder()
                .originalUserId(user.getUserId())
                .maskedEmail("withdrawn_" + user.getUserId() + "@deleted.com")
                .provider(user.getProvider())
                .withdrawnAt(LocalDateTime.now())
                .signupAt(user.getCreatedAt())
                .totalDrinkDays(totalDrinkDays)
                .dataRetentionUntil(LocalDateTime.now().plusMonths(3))
                .build();
    }
}
