package com.umc.puppymode2.global.security;

import com.umc.puppymode2.global.apiPayload.code.status.ErrorStatus;
import com.umc.puppymode2.global.auth.token.JwtTokenProvider;
import com.umc.puppymode2.global.util.ErrorResponseUtil;
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
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();

        return matchesAny(uri, SecurityConfig.AUTH_WHITELIST)
                || matchesAny(uri, SecurityConfig.HEALTH_WHITELIST)
                || matchesAny(uri, SecurityConfig.SWAGGER_WHITELIST)
                || matchesAny(uri, SecurityConfig.PUBLIC_WHITELIST)
                || matchesAny(uri, SecurityConfig.SYSTEM_WHITELIST);
    }

    private boolean matchesAny(String uri, String[] patterns) {
        for (String pattern : patterns) {
            if (pathMatcher.match(pattern, uri)) {
                return true;
            }
        }
        return false;
    }

    /**
     * JWT 인증 필터 처리 메서드
     * <p>
     * Access Token을 추출하고, 토큰 유효성을 검증합니다.
     * SecurityContext에 인증 정보를 등록합니다.
     * 유효하지 않은 토큰의 경우 {@link ErrorResponseUtil} 을 통해 인증 실패 응답을 반환합니다.
     *
     * @param request
     * @param response
     * @param filterChain
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            if (isMeaningfulProtectedPath(requestURI)) { // list에 있는 것만 로그 출력
                log.warn("[JWT] Missing Authorization - URI: {}, IP: {}", requestURI, getClientIp(request));
            } else {
                log.debug("[JWT] Missing Authorization - URI: {}, IP: {}", requestURI, getClientIp(request));
            }
            sendUnauthorized(response);
            return;
        }

        String token = header.substring(7);
        if (token.isBlank()) {
            log.warn("[JWT] Empty token - URI: {}, IP: {}", requestURI, getClientIp(request));
            sendUnauthorized(response);
            return;
        }

        try {
            JwtValidationType result = jwtTokenProvider.validateToken(token);
            if (result != JwtValidationType.VALID_JWT) {
                log.warn("[JWT] Invalid token - Type: {}, URI: {}, IP: {}",
                        result.name(), requestURI, getClientIp(request));
                sendUnauthorized(response);
                return;
            }

            Long userId = jwtTokenProvider.parseUserId(token);
            UserAuthentication authentication = new UserAuthentication(userId, null, null);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.info("[JWT] Request - UID: {}, URI: {}", userId, requestURI);

            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.error("[JWT] Unexpected error - URI: {}", requestURI, e);
            sendUnauthorized(response);
        }
    }

    /**
     * URI가 일반 API 요청인지 확인합니다.
     *
     * @param uri
     * @return
     */
    private boolean isMeaningfulProtectedPath(String uri) {
        return uri.startsWith("/auth/")
                || uri.startsWith("/user/")
                || uri.startsWith("/advice")
                || uri.startsWith("/puppy-name")
                || uri.startsWith("/my-name")
                || uri.startsWith("/main")
                || uri.startsWith("/calendar")
                || uri.startsWith("/drink-history")
                || uri.startsWith("/goals")
                || uri.startsWith("/report")
                || uri.startsWith("/api")
                || uri.startsWith("/version");
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip != null ? ip : "unknown";
    }

    /**
     * 인증 실패 응답을 클라이언트에 전송합니다.
     * <p>
     * 공통 응답 포맷 {@link com.umc.puppymode2.global.apiPayload.ApiResponse} 형식을 사용합니다.
     *
     * @param response
     * @throws IOException
     */
    private void sendUnauthorized(HttpServletResponse response) throws IOException {
        SecurityContextHolder.clearContext();
        ErrorResponseUtil.writeErrorResponse(response, ErrorStatus.AUTH_INVALID_TOKEN);
    }
}
