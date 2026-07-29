package com.umc.puppymode2.domain.notification.service;

import com.google.firebase.messaging.*;
import com.umc.puppymode2.domain.notification.dto.DrinkReminderMessage;
import com.umc.puppymode2.domain.notification.dto.DrinkReminderTarget;
import com.umc.puppymode2.domain.notification.repository.FcmTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
// TODO: iOS 알림 미수신 원인 파악(#171) 완료 후, 아래 상세 로그(log.info/warn/error) 및 maskUsername() 제거 필요
public class FcmSender {

    private final FcmTokenRepository fcmTokenRepository;

    private static final int FCM_BATCH_SIZE = 500;

    /**
     * 유저별 개인화 메시지 전송
     * sendEach 사용 → 메시지마다 내용이 달라도 한 번의 HTTP 요청으로 처리
     */
    @Transactional
    public FcmSendResult sendPersonalized(List<DrinkReminderTarget> targets) {
        if (targets.isEmpty()) return new FcmSendResult(0, 0);

        log.info("[FCM] 전송 시작 - 총 {}건", targets.size());

        int totalSuccess = 0;
        int totalFailure = 0;

        for (List<DrinkReminderTarget> batch : partition(targets, FCM_BATCH_SIZE)) {
            List<Message> messages = batch.stream()
                    .map(target -> Message.builder()
                            .setToken(target.fcmToken())
                            .setNotification(Notification.builder()
                                    .setTitle(DrinkReminderMessage.getTitle())
                                    .setBody(DrinkReminderMessage.getBody(target.username()))
                                    .build())
                            .putData("landing", "drink_history")
                            .setAndroidConfig(AndroidConfig.builder()
                                    .setPriority(AndroidConfig.Priority.HIGH)
                                    .build())
                            .setApnsConfig(ApnsConfig.builder()
                                    .setAps(Aps.builder()
                                            .setSound("default")
                                            .build())
                                    .build())
                            .build())
                    .toList();

            try {
                BatchResponse response = FirebaseMessaging.getInstance().sendEach(messages);
                log.info("[FCM] 배치 전송 완료 - 성공: {}, 실패: {}",
                        response.getSuccessCount(), response.getFailureCount());

                totalSuccess += response.getSuccessCount();
                totalFailure += response.getFailureCount();

                // 성공 건: messageId를 남겨야 Firebase/APNs 쪽에서 실제 도달 여부 추적 가능
                List<SendResponse> responses = response.getResponses();
                for (int i = 0; i < responses.size(); i++) {
                    if (responses.get(i).isSuccessful()) {
                        log.info("[FCM] 전송 성공 - user: {}, messageId: {}",
                                maskUsername(batch.get(i).username()), responses.get(i).getMessageId());
                    }
                }

                if (response.getFailureCount() > 0) {
                    handleFailures(batch, response);
                }
            } catch (FirebaseMessagingException e) {
                log.error("[FCM] 배치 전송 실패", e);
                totalFailure += batch.size();
            }
        }

        return new FcmSendResult(totalSuccess, totalFailure);
    }

    private void handleFailures(List<DrinkReminderTarget> targets, BatchResponse response) {
        List<SendResponse> responses = response.getResponses();
        for (int i = 0; i < responses.size(); i++) {
            if (!responses.get(i).isSuccessful()) {
                FirebaseMessagingException ex = responses.get(i).getException();
                MessagingErrorCode code = ex.getMessagingErrorCode();

                log.warn("[FCM] 토큰 전송 실패 - user: {}, errorCode: {}, message: {}",
                        maskUsername(targets.get(i).username()), code, ex.getMessage());

                if (code == MessagingErrorCode.UNREGISTERED
                        || code == MessagingErrorCode.INVALID_ARGUMENT) {
                    log.warn("[FCM] 만료 토큰 삭제 - user: {}", maskUsername(targets.get(i).username()));
                    fcmTokenRepository.deleteByFcmToken(targets.get(i).fcmToken());
                }

                // APNs 인증키 오류 (Firebase 콘솔 APNs 인증키 확인 필요)
                if (code == MessagingErrorCode.THIRD_PARTY_AUTH_ERROR) {
                    log.error("[FCM] APNs 인증 오류 - user: {}, code: {} (Firebase 콘솔 APNs 인증키 확인 필요)",
                            maskUsername(targets.get(i).username()), code);
                }

                // sender ID 불일치 (다른 프로젝트에서 발급된 토큰일 가능성)
                if (code == MessagingErrorCode.SENDER_ID_MISMATCH) {
                    log.error("[FCM] Sender ID 불일치 - user: {}, code: {} (토큰 발급 프로젝트 확인 필요)",
                            maskUsername(targets.get(i).username()), code);
                }
            }
        }
    }

    // TODO: iOS 알림 미수신 원인 파악 완료 후 아래 로그 및 마스킹 로직 제거 (#171)
    /**
     * 운영 로그 - 실명/닉네임 마스킹
     */
    private String maskUsername(String username) {
        if (username == null || username.isBlank()) return "unknown";
        if (username.length() <= 2) {
            return username.charAt(0) + "*";
        }
        return username.charAt(0) + "*".repeat(username.length() - 2) + username.charAt(username.length() - 1);
    }

    private <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
    }

    public record FcmSendResult(int successCount, int failureCount) {}
}