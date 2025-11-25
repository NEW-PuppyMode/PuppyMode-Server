package com.umc.puppymode2.domain.version.entity;

import com.umc.puppymode2.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "app_version")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppVersion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long versionId;

    @Column(nullable = false, unique = true)
    private String osType;

    @Column(nullable = false)
    private String latestVersion;

    private String updateUrl;

    @Builder
    public AppVersion(String osType,
                      String latestVersion,
                      String updateUrl) {
        this.osType = osType;
        this.latestVersion = latestVersion;
        this.updateUrl = updateUrl;
    }
}