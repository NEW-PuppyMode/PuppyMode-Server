package com.umc.puppymode2.domain.notification.controller;

import com.umc.puppymode2.domain.notification.dto.DrinkReminderTarget;
import com.umc.puppymode2.domain.notification.repository.FcmTokenRepository;
import com.umc.puppymode2.domain.notification.service.FcmSender;
import com.umc.puppymode2.global.apiPayload.ApiResponse;
import com.umc.puppymode2.global.apiPayload.code.status.SuccessStatus;
import com.umc.puppymode2.global.auth.context.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "notification-test-controller", description = "알림 테스트 API")
@RestController
@RequestMapping("/notifications/test")
@RequiredArgsConstructor
public class NotificationTestController {

    private final UserContext userContext;
    private final FcmTokenRepository fcmTokenRepository;
    private final FcmSender fcmSender;

    @Operation(summary = "[테스트] 음주 알림 즉시 발송", description = "22시 스케줄러 대신 현재 유저에게 즉시 알림을 발송합니다.")
    @PostMapping("/drink-reminder")
    public ResponseEntity<ApiResponse<Void>> sendDrinkReminderNow() {
        Long userId = userContext.getCurrentUserId();

        List<DrinkReminderTarget> targets = fcmTokenRepository.findByUserUserId(userId)
                .stream()
                .map(token -> new DrinkReminderTarget(token.getFcmToken(), token.getUser().getUsername()))
                .toList();

        fcmSender.sendPersonalized(targets);

        return ResponseEntity.ok(ApiResponse.onSuccess(
                null,
                SuccessStatus.NOTIFICATION_TEST_SEND_SUCCESS.getCode(),
                SuccessStatus.NOTIFICATION_TEST_SEND_SUCCESS.getMessage()
        ));
    }
}