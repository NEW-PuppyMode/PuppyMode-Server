package com.umc.puppymode2.global.security;

import com.umc.puppymode2.global.auth.token.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static com.umc.puppymode2.global.security.JwtValidationType.VALID_JWT;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();

        // 헬스체크 경로는 필터 자체를 스킵
        if (requestURI.equals("/actuator/health")) {
            log.info("[JwtAuthenticationFilter] 요청된 헬스체크(/actuator/health)에 대한 필터 검증을 건너뜁니다.");
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String token = getJwtFromRequest(request);
//            log.debug("Extracted JWT: {}", token);

            if (token == null) {
                filterChain.doFilter(request, response);
                return;
            }

            if (jwtTokenProvider.validateToken(token) == VALID_JWT) {
                Long userId = jwtTokenProvider.getUserFromJwt(token);
                // authentication 객체 생성 -> principal에 유저정보를 담는다.
                UserAuthentication authentication = new UserAuthentication(userId, null, null);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception exception) {
            log.error("JWT processing error: {}", exception.getMessage());
        }
        // 다음 필터로 요청 전달
        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        log.debug("Authorization Header: {}", bearerToken);

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring("Bearer ".length());
//            log.debug("Extracted JWT from request: {}", token);
            return token;
        }

        log.warn("Authorization header is missing or invalid");
        return null;
    }
}
