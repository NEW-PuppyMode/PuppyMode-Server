package com.umc.puppymode2.global.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomJwtAuthenticationEntryPoint customJwtAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    private static final String[] AUTH_WHITELIST = {
            "/auth/kakao/login/**",
            "/auth/apple/login/**",
            "/auth/apple/callback",
            "/auth/apple/callback/**",
    };

    private static final String[] HEALTH_WHITELIST = {
            "/actuator/health"
    };

    private static final String[] SWAGGER_WHITELIST = {
            "/swagger-ui/**",
            "/v3/api-docs/**"
    };

    private static final String[] PUBLIC_WHITELIST = {
            "/account-deletion",
            "/privacy-policy"
    };

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .formLogin(formLogin -> formLogin.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .exceptionHandling(exception -> {
                    exception.authenticationEntryPoint(customJwtAuthenticationEntryPoint);
                    exception.accessDeniedHandler(customAccessDeniedHandler);
                })
                .authorizeHttpRequests(auth -> {
                    // 공격 패턴 차단
                    auth.requestMatchers("/**/*.php").denyAll();
                    auth.requestMatchers("/**/*.asp", "/**/*.aspx").denyAll();
                    auth.requestMatchers("/wp-admin/**", "/wp-content/**", "/wp-includes/**").denyAll();
                    auth.requestMatchers("/phpMyAdmin/**", "/phpmyadmin/**").denyAll();
                    auth.requestMatchers("/**/.env", "/**/.git/**", "/**/.svn/**").denyAll();
                    auth.requestMatchers("/vendor/**").denyAll();

                    // 정상 경로
                    auth.requestMatchers(AUTH_WHITELIST).permitAll();
                    auth.requestMatchers(HEALTH_WHITELIST).permitAll();
                    auth.requestMatchers(SWAGGER_WHITELIST).permitAll();
                    auth.requestMatchers(PUBLIC_WHITELIST).permitAll();
                    auth.anyRequest().authenticated();
                })
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
