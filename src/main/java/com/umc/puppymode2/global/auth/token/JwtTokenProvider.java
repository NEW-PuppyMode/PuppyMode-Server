package com.umc.puppymode2.global.auth.token;

import com.umc.puppymode2.global.security.JwtValidationType;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private static final String USER_ID = "userId";
    private static final Duration ACCESS_TOKEN_EXPIRE_TIME = Duration.ofHours(1);
    private static final Duration REFRESH_TOKEN_EXPIRE_TIME = Duration.ofDays(14);

    @Value("${auth.jwt.secret}")
    private String JWT_SECRET;

    private SecretKey signingKey;

    @PostConstruct
    protected void init() {
        byte[] decodedKey = Base64.getUrlDecoder().decode(JWT_SECRET);
        this.signingKey = Keys.hmacShaKeyFor(decodedKey);
        log.debug("[JWT] Signing key initialized (Base64-decoded)");
    }

    /**
     * AccessToken을 생성합니다.
     */
    public String generateAccessToken(Long userId) {
        return generateToken(userId, ACCESS_TOKEN_EXPIRE_TIME, "ACCESS");
    }

    /**
     * RefreshToken을 생성합니다.
     */
    public String generateRefreshToken(Long userId) {
        return generateToken(userId, REFRESH_TOKEN_EXPIRE_TIME, "REFRESH");
    }

    /**
     * JWT를 생성합니다.
     *
     * @param userId
     * @param expiry
     * @param tokenType
     * @return
     */
    public String generateToken(Long userId, Duration expiry, String tokenType) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiry.toMillis());

        final Claims claims = Jwts.claims()
                .setIssuedAt(now)
                .setExpiration(expiryDate);  // 만료 시간 설정
        claims.put(USER_ID, userId);

//        log.info("Claims before signing: {}", claims); // JWT 생성 전 Claims 확인

        String jwt = Jwts.builder()
                .setHeaderParam(Header.TYPE, Header.JWT_TYPE) // Header
                .setClaims(claims) // Claim
                .signWith(getSigningKey()) // Signature
                .compact();

        log.info("[JWT] {} Token 생성 - userId={}, 만료시각: {}", tokenType, userId, expiryDate);
        return jwt;
    }

    private SecretKey getSigningKey() {
        return signingKey;
    }

    /**
     * 토큰의 유효성을 검증합니다.
     *
     * @param token
     * @return
     */
    public JwtValidationType validateToken(String token) {
//        log.info("JWT Validation Result: {}", token);
        try {
            final Claims claims = getTokenBody(token);
            return JwtValidationType.VALID_JWT;
        } catch (SignatureException ex) {
            return JwtValidationType.INVALID_JWT_SIGNATURE;
        } catch (MalformedJwtException ex) {
            return JwtValidationType.INVALID_JWT_TOKEN;
        } catch (ExpiredJwtException ex) {
            return JwtValidationType.EXPIRED_JWT_TOKEN;
        } catch (UnsupportedJwtException ex) {
            return JwtValidationType.UNSUPPORTED_JWT_TOKEN;
        } catch (IllegalArgumentException ex) {
            return JwtValidationType.EMPTY_JWT;
        }
    }

    /**
     * JWT에서 userId를 추출합니다.
     */
    public Long parseUserId(String token) {
        if (!StringUtils.hasText(token)) {
            throw new IllegalArgumentException("JWT is null or empty");
        }

        Claims claims = getTokenBody(token);
        return extractUserId(claims);
    }

    /**
     * userId 를 파싱합니다.
     */
    private Long extractUserId(Claims claims) {
        Object userIdObj = claims.get(USER_ID);
        if (userIdObj == null) {
            throw new IllegalArgumentException("Invalid JWT: missing userId");
        }
        try {
            return Long.parseLong(userIdObj.toString());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid userId format in JWT");
        }
    }

    /**
     * JWT Body를 추출합니다. (Claims 파싱)
     */
    private Claims getTokenBody(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 액세스 토큰 만료 시간을 초 단위로 반환합니다.
     */
    public Long getAccessTokenExpirySeconds() {
        return ACCESS_TOKEN_EXPIRE_TIME.toSeconds();
    }

}

