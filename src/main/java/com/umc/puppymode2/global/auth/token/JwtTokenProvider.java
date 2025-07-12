package com.umc.puppymode2.global.auth.token;

import com.umc.puppymode2.global.security.JwtValidationType;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private static final String USER_ID = "userId";
    private static final Long TOKEN_EXPIRATION_TIME = 24 * 60 * 60 * 1000L;

    @Value("${auth.jwt.secret}")
    private String JWT_SECRET;

    @PostConstruct
    protected void init() {
        //base64 라이브러리에서 encodeToString을 이용해서 byte[] 형식을 String 형식으로 변환
        JWT_SECRET = Base64.getEncoder().encodeToString(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Authentication authentication) {
        final Date now = new Date();

        final Claims claims = Jwts.claims()
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + TOKEN_EXPIRATION_TIME));  // 만료 시간 설정

        claims.put(USER_ID, authentication.getPrincipal());

//        log.info("Claims before signing: {}", claims); // JWT 생성 전 Claims 확인
        return Jwts.builder()
                .setHeaderParam(Header.TYPE, Header.JWT_TYPE) // Header
                .setClaims(claims) // Claim
                .signWith(getSigningKey()) // Signature
                .compact();
    }

    private SecretKey getSigningKey() {
        String encodedKey = Base64.getEncoder().encodeToString(JWT_SECRET.getBytes()); //SecretKey 통해 서명 생성
        return Keys.hmacShaKeyFor(encodedKey.getBytes());
    }

    public JwtValidationType validateToken(String token) {
//        log.info("JWT Validation Result: {}", token);
        try {
            final Claims claims = getBody(token);
            return JwtValidationType.VALID_JWT;
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

    private Claims getBody(final String token) {
//        log.info("Parsing JWT: {}", token);
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public Long getUserFromJwt(String token) {
        if (!StringUtils.hasText(token)) {
//            log.error("JWT is null or empty");
            throw new IllegalArgumentException("JWT is null or empty");
        }

        Claims claims = getBody(token);
//        log.info("Parsed Claims: {}", claims);

        if (claims.get(USER_ID) == null) {
//            log.error("JWT does not contain userId");
            throw new IllegalArgumentException("Invalid JWT: missing userId");
        }

        return Long.valueOf(claims.get("userId").toString());
    }


}

