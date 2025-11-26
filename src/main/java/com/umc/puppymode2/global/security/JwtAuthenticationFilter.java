package com.umc.puppymode2.global.security;

import com.umc.puppymode2.global.apiPayload.code.status.ErrorStatus;
import com.umc.puppymode2.global.auth.token.JwtTokenProvider;
import com.umc.puppymode2.global.util.ErrorResponseUtil;
import com.umc.puppymode2.global.exception.GeneralException;
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
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    // no auth 허용 url
    private static final List<String> NO_AUTH_URLS = List.of(
            "/auth/kakao/login/**",
            "/actuator/health",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/account-deletion",
            "/privacy-policy"
    );

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
        log.info("Request URI: {}", request.getRequestURI());

        // 인증이 필요 없는 URI 필터 통과
        if (isExcludedPath(requestURI)) {
            log.debug("[JWT] 필터 제외 대상: {}", requestURI);
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");
        // 401 error
        if (header == null || !header.startsWith("Bearer ")) {
            log.warn("[JWT] Authorization 헤더 없음 또는 형식 오류 - IP: {}", request.getRemoteAddr());
            sendUnauthorized(response);
            return;
        }

        // JWT 추출
        String token = header.substring(7);
        if (token.isBlank()) {
            log.warn("[JWT] 토큰이 비어 있음");
            sendUnauthorized(response);
            return;
        }

        try {
//            log.debug("Extracted JWT: {}", token);

            // validate
            JwtValidationType result = jwtTokenProvider.validateToken(token);

            if (result != JwtValidationType.VALID_JWT) {
                // Invalid token 관련 디테일 로그
                log.warn("[JWT] 인증 실패 - validationType: {}, user-agent: {}", result.name(), request.getHeader("User-Agent"));
                throw new GeneralException(ErrorStatus.AUTH_INVALID_TOKEN);
            }

            Long userId = jwtTokenProvider.parseUserId(token);
            // authentication 생성 -> principal에 userId 저장
            UserAuthentication authentication = new UserAuthentication(userId, null, null);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);

        } catch (GeneralException exception) {
            // Invalid token 관련 응답 통일
            sendUnauthorized(response);
        } catch (Exception exception) {
            log.error("JWT 처리 중 알 수 없는 에러: {}", exception.getMessage(), exception);
            // Invalid token 관련 응답 통일
            sendUnauthorized(response);
        }
    }

    /**
     * URI가 인증 제외 경로인지 확인합니다.
     *
     * @param requestURI
     * @return
     */
    private boolean isExcludedPath(String requestURI) {
        return NO_AUTH_URLS.stream().anyMatch(pattern -> pathMatcher.match(pattern, requestURI));
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
        SecurityContextHolder.clearContext(); // 인증 정보 제거
        ErrorResponseUtil.writeErrorResponse(response, ErrorStatus.AUTH_INVALID_TOKEN);
    }
}
