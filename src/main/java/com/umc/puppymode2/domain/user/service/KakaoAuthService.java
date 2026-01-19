package com.umc.puppymode2.domain.user.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.puppymode2.domain.user.auth.dto.KakaoTokenResponseDTO;
import com.umc.puppymode2.domain.user.auth.dto.KakaoUserInfoResponseDTO;
import com.umc.puppymode2.domain.user.repository.SocialAuthRepository;
import com.umc.puppymode2.domain.user.auth.dto.UserAuthInfoDTO;
import com.umc.puppymode2.global.apiPayload.code.status.ErrorStatus;
import com.umc.puppymode2.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoAuthService {

    private final SocialAuthRepository socialAuthRepository;
    private final ObjectMapper objectMapper;

    @Qualifier("kakaoWebClient")
    private final WebClient kakaoWebClient;

    @Qualifier("kakaoTokenWebClient")
    private final WebClient kakaoTokenWebClient;

    @Value("${auth.kakao.rest-api-key}")
    private String kakaoRestApiKey;

    /**
     * 카카오 회원의 정보를 가져옵니다.
     *
     * @param accessToken 카카오 액세스 토큰
     * @return 회원 정보
     * @throws GeneralException 카카오 API 호출 실패 시
     */
    public UserAuthInfoDTO getUserInfo(String accessToken) {
        KakaoUserInfoResponseDTO userInfo = fetchKakaoUserInfo(accessToken);
        validateKakaoUserInfo(userInfo);

        return UserAuthInfoDTO.builder()
                .providerId(userInfo.getId().toString())
                .email(userInfo.getKakaoAccount().getEmail())
                .username(userInfo.getKakaoAccount().getProfile().getNickName())
                .build();
    }

    /**
     * refreshToken을 사용하여 새로운 accessToken을 발급합니다.
     *
     * @param userId 사용자 ID
     * @return 새로운 액세스 토큰
     * @throws GeneralException refresh token이 없거나 갱신 실패 시
     */
    public String refreshAccessToken(Long userId) {
        String refreshToken = getRefreshTokenFromDB(userId);
        KakaoTokenResponseDTO tokenResponse = requestNewAccessToken(refreshToken);

        validateTokenResponse(tokenResponse);
        updateRefreshTokenIfPresent(userId, tokenResponse.getRefreshToken());

        return tokenResponse.getAccessToken();
    }

    /**
     * 카카오 계정 연결 끊기(회원탈퇴)
     */
    public boolean disconnectKakao(String accessToken) {
        try {
            kakaoWebClient.post()
                    .uri("/v1/user/unlink")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, response ->
                            response.bodyToMono(String.class).map(errorBody -> {
                                log.error("[Kakao disconnect] API 에러 응답: {}", errorBody);
                                throw new RuntimeException("카카오 계정 연결 해제 실패: " + errorBody);
                            })
                    )
                    .bodyToMono(Void.class)
                    .block();

            log.info("[Kakao disconnect] 카카오 계정 연결 해제 성공");
            return true;
        } catch (Exception e) {
            log.error("[Kakao disconnect] 카카오 계정 연결 해제 중 예외 발생", e);
            return false;
        }
    }

    // ==================== Private Methods ====================

    /**
     * 카카오 API를 통해 사용자 정보를 조회합니다.
     */
    private KakaoUserInfoResponseDTO fetchKakaoUserInfo(String accessToken) {
        KakaoUserInfoResponseDTO userInfo = kakaoWebClient.get()
                .uri("/v2/user/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .onStatus(HttpStatusCode::isError,
                        response -> handleKakaoError(response, "GET_USER_INFO"))
                .bodyToMono(KakaoUserInfoResponseDTO.class)
                .block();

        if (userInfo == null) {
            log.error("[Kakao Service] GET_USER_INFO - 응답이 null");
            throw new GeneralException(ErrorStatus.KAKAO_API_ERROR);
        }

        log.debug("[Kakao Service] GET_USER_INFO 성공 - userId: {}", userInfo.getId());
        return userInfo;
    }

    /**
     * DB에서 refresh token을 조회합니다.
     */
    private String getRefreshTokenFromDB(Long userId) {
        return socialAuthRepository.findRefreshTokenByUserId(userId)
                .orElseThrow(() -> {
                    log.error("[Kakao Service] Refresh token not found - userId: {}", userId);
                    return new GeneralException(ErrorStatus.AUTH_REFRESH_TOKEN_INVALID);
                });
    }

    /**
     * 카카오 API를 통해 새로운 액세스 토큰을 요청합니다.
     */
    private KakaoTokenResponseDTO requestNewAccessToken(String refreshToken) {
        KakaoTokenResponseDTO tokenResponse = kakaoTokenWebClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/oauth/token")
                        .queryParam("grant_type", "refresh_token")
                        .queryParam("client_id", kakaoRestApiKey)
                        .queryParam("refresh_token", refreshToken)
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::isError,
                        response -> handleKakaoError(response, "REFRESH_TOKEN"))
                .bodyToMono(KakaoTokenResponseDTO.class)
                .block();

        log.debug("[Kakao Service] REFRESH_TOKEN API 호출 완료");
        return tokenResponse;
    }

    /**
     * 토큰 응답의 유효성을 검증합니다.
     */
    private void validateTokenResponse(KakaoTokenResponseDTO tokenResponse) {
        if (tokenResponse == null || tokenResponse.getAccessToken() == null) {
            log.error("[Kakao Service] REFRESH_TOKEN 실패 - 응답 null 또는 access_token 없음");
            throw new GeneralException(ErrorStatus.KAKAO_TOKEN_REFRESH_FAILED);
        }
    }

    /**
     * 새로운 refresh token이 있으면 DB에 업데이트합니다.
     */
    private void updateRefreshTokenIfPresent(Long userId, String newRefreshToken) {
        Optional.ofNullable(newRefreshToken).ifPresent(token -> {
            socialAuthRepository.updateRefreshToken(userId, token);
            log.debug("[Kakao Service] Refresh token 업데이트 완료 - userId: {}", userId);
        });
    }

    /**
     * 카카오 사용자 정보의 필수 필드를 검증합니다.
     *
     * @throws GeneralException 필수 정보가 없을 경우
     */
    private void validateKakaoUserInfo(KakaoUserInfoResponseDTO userInfo) {
        if (userInfo.getId() == null) {
            log.error("[Kakao Service] 카카오 고유 ID(auth_id) null");
            throw new GeneralException(ErrorStatus.KAKAO_USER_INFO_INVALID);
        }

        if (userInfo.getKakaoAccount() == null) {
            log.error("[Kakao Service] 카카오 계정 정보 null");
            throw new GeneralException(ErrorStatus.KAKAO_USER_INFO_INVALID);
        }

        if (userInfo.getKakaoAccount().getProfile() == null ||
                userInfo.getKakaoAccount().getProfile().getNickName() == null) {
            log.error("[Kakao Service] 카카오 닉네임 정보 null");
            throw new GeneralException(ErrorStatus.KAKAO_USER_INFO_INVALID);
        }

        if (userInfo.getKakaoAccount().getEmail() == null) {
            log.error("[Kakao Service] 카카오 이메일 정보 null");
            throw new GeneralException(ErrorStatus.KAKAO_USER_INFO_INVALID);
        }
    }

    /**
     * WebClient 에러 응답을 처리하고 적절한 예외로 변환합니다.
     */
    private Mono<? extends Throwable> handleKakaoError(ClientResponse response, String operation) {
        return response.bodyToMono(String.class)
                .defaultIfEmpty("")
                .flatMap(errorBody -> {
                    log.error("[Kakao Service] {} 실패 - status: {}", operation, response.statusCode());
                    log.debug("[Kakao Service] {} 에러 상세: {}", operation, sanitize(errorBody));

                    return Mono.error(mapKakaoError(errorBody));
                });
    }

    /**
     * 카카오 에러 코드를 애플리케이션 예외로 매핑합니다.
     */
    private static final Set<Integer> BAD_REQUEST_CODES =
            Set.of(-2, -201, -101, -102, -103, -501, -602, -606, -911);

    private static final Set<Integer> FORBIDDEN_CODES =
            Set.of(-3, -402);

    private static final Pattern KAKAO_CODE_PATTERN =
            Pattern.compile("\"code\"\\s*:\\s*(-?\\d+)");

    private GeneralException mapKakaoError(String errorBody) {
        int code = extractCode(errorBody);

        if (code == -401 || code == -903) return new GeneralException(ErrorStatus.KAKAO_TOKEN_INVALID);
        if (BAD_REQUEST_CODES.contains(code)) return new GeneralException(ErrorStatus.KAKAO_BAD_REQUEST);
        if (FORBIDDEN_CODES.contains(code)) return new GeneralException(ErrorStatus.KAKAO_FORBIDDEN);
        if (code == -603) return new GeneralException(ErrorStatus.KAKAO_TIMEOUT);
        if (code == -9798) return new GeneralException(ErrorStatus.KAKAO_SERVICE_UNAVAILABLE);
        if (code == -10) return new GeneralException(ErrorStatus.KAKAO_RATE_LIMITED);

        return new GeneralException(ErrorStatus.KAKAO_API_ERROR);
    }

    private int extractCode(String body) {
        if (body == null || body.isBlank()) return 0;

        Matcher m = KAKAO_CODE_PATTERN.matcher(body);
        if (!m.find()) return 0;

        try {
            return Integer.parseInt(m.group(1));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 민감한 정보(토큰 등)를 마스킹 처리합니다.
     */
    private String sanitize(String raw) {
        if (raw == null || raw.isBlank()) {
            return raw;
        }

        return raw
                .replaceAll("\"access_token\"\\s*:\\s*\"[^\"]+\"", "\"access_token\":\"***\"")
                .replaceAll("\"refresh_token\"\\s*:\\s*\"[^\"]+\"", "\"refresh_token\":\"***\"")
                .replaceAll("\"id_token\"\\s*:\\s*\"[^\"]+\"", "\"id_token\":\"***\"");
    }
}