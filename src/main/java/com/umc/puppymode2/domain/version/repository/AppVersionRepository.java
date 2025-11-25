package com.umc.puppymode2.domain.version.repository;

import com.umc.puppymode2.domain.version.entity.AppVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AppVersionRepository extends JpaRepository<AppVersion, Long> {
    Optional<AppVersion> findByOsType(String osType);
}