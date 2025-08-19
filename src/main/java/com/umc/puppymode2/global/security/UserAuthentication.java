package com.umc.puppymode2.global.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class UserAuthentication extends UsernamePasswordAuthenticationToken {

    /**
     * 인증된 사용자를 생성합니다.
     *
     * @param userId      사용자 ID (Principal로 사용)
     * @param credentials 인증 자격 정보 (일반적으로 null)
     * @param authorities 권한 목록 (ROLE_USER 등)
     */
    public UserAuthentication(Long userId,
                              Object credentials,
                              Collection<? extends GrantedAuthority> authorities) {
        super(userId, credentials, authorities);
    }

    /**
     * 인증된 사용자 ID를 반환합니다.
     *
     * @return userId (Long)
     * @throws IllegalStateException principal이 Long이 아닐 경우
     */
    public Long getUserId() {
        Object principal = getPrincipal();
        if (principal instanceof Long) {
            return (Long) principal;
        }
        throw new IllegalStateException("Authentication principal is not a Long (userId).");
    }
}
