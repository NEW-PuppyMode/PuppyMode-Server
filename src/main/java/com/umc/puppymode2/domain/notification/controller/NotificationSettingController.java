package com.umc.puppymode2.domain.notification.controller;

import com.umc.puppymode2.domain.notification.dto.NotificationSettingRequestDTO;
import com.umc.puppymode2.domain.notification.dto.NotificationSettingResponseDTO;
import com.umc.puppymode2.domain.notification.service.NotificationSettingService;
import com.umc.puppymode2.global.apiPayload.ApiResponse;
import com.umc.puppymode2.global.apiPayload.code.status.SuccessStatus;
import com.umc.puppymode2.global.auth.context.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name="notification-setting-controller", description = "알림 수신 여부 설정 API")
@RestController
@RequestMapping("/notification-settings")
@RequiredArgsConstructor
public class NotificationSettingController {

    private final UserContext userContext;
    private final NotificationSettingService notificationSettingService;

    @Operation(summary = "알림 수신 여부 조회", description = "현재 유저의 알림 수신 여부를 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<NotificationSettingResponseDTO>> getStatus(){
        Long userId = userContext.getCurrentUserId();
        NotificationSettingResponseDTO result = notificationSettingService.getStatus(userId);
        return ResponseEntity.ok(ApiResponse.onSuccess(
                result,
                SuccessStatus.NOTIFICATION_STATUS_SUCCESS.getCode(),
                SuccessStatus.NOTIFICATION_STATUS_SUCCESS.getMessage()
                )
        );
    }

    @Operation(summary = "알림 수신 여부 변경", description = "알림 수신 여부를 변경합니다.")
    @PatchMapping
    public ResponseEntity<ApiResponse<NotificationSettingResponseDTO>> update(
            @Valid @RequestBody NotificationSettingRequestDTO request) {
        Long userId = userContext.getCurrentUserId();
        NotificationSettingResponseDTO result = notificationSettingService.update(userId, request.getReceiveNotifications());
        return ResponseEntity.ok(ApiResponse.onSuccess(
                result,
                SuccessStatus.NOTIFICATION_UPDATE_SUCCESS.getCode(),
                SuccessStatus.NOTIFICATION_UPDATE_SUCCESS.getMessage()
                )
        );
    }
}
