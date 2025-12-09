package com.umc.puppymode2.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean("kakaoWebClient")
    public WebClient kakaoWebClient() {
        return WebClient.builder()
                .baseUrl("https://kapi.kakao.com") // 사용자 정보 조회
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Bean("kakaoTokenWebClient")
    public WebClient kakaoTokenWebClient() {
        return WebClient.builder()
                .baseUrl("https://kauth.kakao.com") // 토큰 갱신용
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .build();
    }

    @Bean("appleWebClient")
    public WebClient appleWebClient() {
        return WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .build();
    }
}