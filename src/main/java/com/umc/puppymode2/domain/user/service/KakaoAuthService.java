package com.umc.puppymode2.domain.user.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.puppymode2.domain.user.dto.KakaoTokenResponseDTO;
import com.umc.puppymode2.domain.user.dto.KakaoUserInfoResponseDTO;
import com.umc.puppymode2.domain.user.repository.SocialAuthRepository;
import com.umc.puppymode2.global.auth.dto.UserAuthInfoDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoAuthService {

    private final SocialAuthRepository socialAuthRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Qualifier("kakaoWebClient")
    private final WebClient kakaoWebClient;

    @Qualifier("kakaoTokenWebClient")
    private final WebClient kakaoTokenWebClient;


    @Value("${auth.kakao.rest-api-key}")
    private String kakaoRestApiKey;

    /**
     * 카카오 회원의 정보를 가져옵니다.
     *
     * @param accessToken
     * @return 회원 정보 전체
     */
    public UserAuthInfoDTO getUserInfo(String accessToken) {
        KakaoUserInfoResponseDTO userInfo = kakaoWebClient.get()
                .uri("/v2/user/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class).map(errorBody -> {
                            log.error("[Kakao Service] API 에러 응답: {}", errorBody);
                            handleKakaoApiError(errorBody);
                            return new RuntimeException("카카오 API 실패");
                        })
                )
                .bodyToMono(KakaoUserInfoResponseDTO.class)
                .block();

        log.debug("[Kakao Service] raw 응답: {}", toJson(userInfo));

        log.debug("[ Kakao Service ] Auth ID —> {} ", userInfo.getId());
        log.debug("[ Kakao Service ] NickName —> {} ", userInfo.getKakaoAccount().getProfile().getNickName());
        log.debug("[ Kakao Service ] email —> {} ", userInfo.getKakaoAccount().getEmail());

        validateKakaoUserInfo(userInfo);

        return UserAuthInfoDTO.builder()
                .providerId(userInfo.getId().toString())
                .email(userInfo.getKakaoAccount().getEmail())
                .username(userInfo.getKakaoAccount().getProfile().getNickName())
                .build();
    }

    /**
     * refreshToken을 사용하여 새로운 accessToken을 발급합니다.
     */
    public String refreshAccessToken(Long userId) {
        String refreshToken = socialAuthRepository.findRefreshTokenByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Refresh token not found"));

        KakaoTokenResponseDTO tokenResponse = kakaoTokenWebClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/oauth/token")
                        .queryParam("grant_type", "refresh_token")
                        .queryParam("client_id", kakaoRestApiKey)
                        .queryParam("refresh_token", refreshToken)
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class).map(errorBody -> {
                            log.error("[Kakao Service] API 에러 응답: {}", errorBody);
                            handleKakaoApiError(errorBody);
                            return new RuntimeException("카카오 API 실패");
                        })
                )
                .bodyToMono(KakaoTokenResponseDTO.class)
                .block();
        log.debug("[Kakao Service] raw 응답: {}", toJson(tokenResponse));

        if (tokenResponse == null || tokenResponse.getAccessToken() == null) {
            throw new RuntimeException("Failed to refresh Kakao access token");
        }

        if (tokenResponse.getRefreshToken() != null) {
            updateRefreshTokenInDB(userId, tokenResponse.getRefreshToken());
        }

        return tokenResponse.getAccessToken();
    }

    /**
     * user_auth 테이블의 refreshToken을 업데이트합니다.
     */
    private void updateRefreshTokenInDB(Long userId, String newRefreshToken) {
        socialAuthRepository.updateRefreshToken(userId, newRefreshToken);
        log.debug("[Kakao Service] Refresh token updated");
    }

    /**
     * 카카오 사용자 정보가 null 인지 검사합니다.
     */
    private void validateKakaoUserInfo(KakaoUserInfoResponseDTO userInfo) {
        if (userInfo == null || userInfo.getId() == null) {
            throw new IllegalArgumentException("카카오 계정의 고유 ID(auth_id)가 존재하지 않습니다.");
        }

        if (userInfo.getKakaoAccount() == null) {
            throw new IllegalArgumentException("카카오 계정 정보가 존재하지 않습니다.");
        }

        if (userInfo.getKakaoAccount().getProfile() == null ||
                userInfo.getKakaoAccount().getProfile().getNickName() == null) {
            throw new IllegalArgumentException("카카오 닉네임 정보가 존재하지 않습니다.");
        }

        if (userInfo.getKakaoAccount().getEmail() == null) {
            throw new IllegalArgumentException("카카오 이메일 정보가 존재하지 않습니다.");
        }
    }

    /**
     * 예외 처리
     */
    private void handleKakaoApiError(String errorResponse) {
        if (errorResponse.contains("\"code\": -401")) {
            throw new IllegalArgumentException("잘못된 Access Token입니다. 재로그인이 필요합니다.");
        }
        if (errorResponse.contains("\"code\": -402")) {
            throw new IllegalStateException("카카오 API 사용량 초과. 잠시 후 다시 시도하세요.");
        }
        if (errorResponse.contains("\"code\": -500")) {
            throw new RuntimeException("카카오 서버 내부 오류 발생. 나중에 다시 시도해주세요.");
        }
        throw new RuntimeException("카카오 API 요청 실패: " + errorResponse);
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "[json 변환 실패]";
        }
    }
}