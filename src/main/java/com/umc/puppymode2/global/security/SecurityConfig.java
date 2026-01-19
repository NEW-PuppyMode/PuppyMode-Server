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

    public static final String[] AUTH_WHITELIST = {
            "/auth/kakao/login/**",
            "/auth/apple/login/**",
            "/auth/apple/callback",
            "/auth/apple/callback/**"
    };

    public static final String[] HEALTH_WHITELIST = {
            "/actuator/health"
    };

    public static final String[] SWAGGER_WHITELIST = {
            "/swagger-ui/**",
            "/v3/api-docs/**"
    };

    public static final String[] PUBLIC_WHITELIST = {
            "/account-deletion",
            "/privacy-policy"
    };

    public static final String[] SYSTEM_WHITELIST = {
            "/version/check"
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
                    auth.requestMatchers("/wp-admin/**").denyAll();
                    auth.requestMatchers("/wp-content/**").denyAll();
                    auth.requestMatchers("/wp-includes/**").denyAll();
                    auth.requestMatchers("/phpMyAdmin/**", "/phpmyadmin/**").denyAll();
                    auth.requestMatchers("/.env", "/.env/**").denyAll();
                    auth.requestMatchers("/.git", "/.git/**").denyAll();
                    auth.requestMatchers("/.svn", "/.svn/**").denyAll();
                    auth.requestMatchers("/vendor", "/vendor/**").denyAll();

                    // 정상 경로
                    auth.requestMatchers(AUTH_WHITELIST).permitAll();
                    auth.requestMatchers(HEALTH_WHITELIST).permitAll();
                    auth.requestMatchers(SWAGGER_WHITELIST).permitAll();
                    auth.requestMatchers(PUBLIC_WHITELIST).permitAll();
                    auth.requestMatchers(SYSTEM_WHITELIST).permitAll();

                    auth.anyRequest().authenticated();
                })
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
