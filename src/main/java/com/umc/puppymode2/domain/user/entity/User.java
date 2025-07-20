package com.umc.puppymode2.domain.user.entity;

import com.umc.puppymode2.domain.common.BaseEntity;
import com.umc.puppymode2.global.auth.enums.Provider;
import com.umc.puppymode2.domain.user.entity.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "`user`")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    private String username;

    private String email;

    @Enumerated(EnumType.STRING)
    private Provider provider;

    private Boolean receiveNotifications;

    @Enumerated(EnumType.STRING)
    private UserStatus status;

    @Column(name = "is_custom_name", nullable = false)
    private boolean isCustomName = false;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SocialAuth> socialAuths = new ArrayList<>();

    public void addSocialAuth(SocialAuth socialAuth) {
        socialAuths.add(socialAuth);
        socialAuth.setUser(this);
    }

    public void removeSocialAuth(SocialAuth socialAuth) {
        socialAuths.remove(socialAuth);
        socialAuth.setUser(null);
    }

    @Builder
    public User(String username,
                String email,
                Provider provider,
                Boolean receiveNotifications,
                UserStatus status,
                boolean isCustomName) {

        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("이름은 필수입니다.");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("이메일은 필수입니다.");
        }
        if (provider == null) {
            throw new IllegalArgumentException("Provider는 필수입니다.");
        }
        if (receiveNotifications == null) {
            throw new IllegalArgumentException("알림 수신 여부는 필수입니다.");
        }
        if (status == null) {
            throw new IllegalArgumentException("상태 값은 필수입니다.");
        }

        this.username = username;
        this.email = email;
        this.provider = provider;
        this.receiveNotifications = receiveNotifications;
        this.status = status;
        this.isCustomName = isCustomName;
    }

    public void setUsername(String username) {
        this.username = username;
        this.isCustomName = true;
    }
}