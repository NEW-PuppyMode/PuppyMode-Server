package com.umc.puppymode2.global.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class UserAuthentication extends UsernamePasswordAuthenticationToken {

    // 사용자 인증 객체 생성
    public UserAuthentication(Long userId, Object credentials, Collection<? extends GrantedAuthority> authorities) {
        super(userId, credentials, authorities);
    }

    public Long getUserId() {
        return (Long) getPrincipal();
    }
}
