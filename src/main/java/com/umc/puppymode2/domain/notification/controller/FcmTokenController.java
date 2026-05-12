package com.umc.puppymode2.domain.notification.controller;

import com.umc.puppymode2.domain.notification.dto.FcmTokenRequestDTO;
import com.umc.puppymode2.domain.notification.service.FcmTokenService;
import com.umc.puppymode2.global.apiPayload.ApiResponse;
import com.umc.puppymode2.global.apiPayload.code.status.SuccessStatus;
import com.umc.puppymode2.global.auth.context.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "fcm-token-controller", description = "FCM 토큰 API")
@RestController
@RequestMapping("/fcm/tokens")
@RequiredArgsConstructor
public class FcmTokenController {

    private final UserContext userContext;
    private final FcmTokenService fcmTokenService;

    @Operation(summary = "FCM 토큰 등록", description = "앱 실행 시 또는 토큰 갱신 시 호출합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody FcmTokenRequestDTO request) {
        Long userId = userContext.getCurrentUserId();
        fcmTokenService.registerToken(userId, request.getFcmToken());
        return ResponseEntity.ok(ApiResponse.onSuccess(null,
                SuccessStatus.FCM_TOKEN_REGISTER_SUCCESS.getCode(),
                SuccessStatus.FCM_TOKEN_REGISTER_SUCCESS.getMessage()));
    }

    @Operation(summary = "FCM 토큰 삭제", description = "로그아웃 시 호출합니다.")
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> remove(@Valid @RequestBody FcmTokenRequestDTO request) {
        Long userId = userContext.getCurrentUserId();
        fcmTokenService.removeToken(userId, request.getFcmToken());
        return ResponseEntity.ok(ApiResponse.onSuccess(null,
                SuccessStatus.FCM_TOKEN_DELETE_SUCCESS.getCode(),
                SuccessStatus.FCM_TOKEN_DELETE_SUCCESS.getMessage()));
    }
}