package com.umc.puppymode2.global.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAuthInfoDTO {
    private String providerId;       // OAuth 제공자의 고유 ID
    private Long userId;
    private String email;
    private String username;
    private LocalDateTime tokenExpiry; // social access token 의 만료일
}
