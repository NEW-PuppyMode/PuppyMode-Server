package com.umc.puppymode2.domain.user.repository;

import com.umc.puppymode2.domain.user.entity.SocialAuth;
import com.umc.puppymode2.domain.user.auth.enums.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface SocialAuthRepository extends JpaRepository<SocialAuth, Long> {

    Optional<SocialAuth> findByUser_EmailAndProvider(String email, Provider provider);

    /**
     * providerId와 Provider로 SocialAuth를 조회합니다.
     * Apple/Kakao 로그인 시 중복 가입 방지를 위해 사용됩니다.
     */
    Optional<SocialAuth> findByProviderIdAndProvider(String providerId, Provider provider);

    @Query("SELECT sa.refreshToken FROM SocialAuth sa WHERE sa.user.userId = :userId")
    Optional<String> findRefreshTokenByUserId(@Param("userId") Long userId);

    @Transactional
    @Modifying
    @Query("UPDATE SocialAuth sa SET sa.refreshToken = :refreshToken WHERE sa.user.userId = :userId")
    void updateRefreshToken(@Param("userId") Long userId, @Param("refreshToken") String refreshToken);

    Optional<SocialAuth> findByUserUserIdAndProvider(Long userId, Provider provider);
}