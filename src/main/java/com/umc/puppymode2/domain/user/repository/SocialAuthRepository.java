package com.umc.puppymode2.domain.user.repository;

import com.umc.puppymode2.domain.user.entity.SocialAuth;
import com.umc.puppymode2.global.auth.enums.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;

import java.util.Optional;

public interface SocialAuthRepository extends JpaRepository<SocialAuth, Long> {

    Optional<SocialAuth> findByUser_EmailAndProvider(String email, Provider provider);

    @Query("SELECT sa.refreshToken FROM SocialAuth sa WHERE sa.user.userId = :userId")
    Optional<String> findRefreshTokenByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE SocialAuth sa SET sa.refreshToken = :refreshToken WHERE sa.user.userId = :userId")
    void updateRefreshToken(@Param("userId") Long userId, @Param("refreshToken") String refreshToken);
}