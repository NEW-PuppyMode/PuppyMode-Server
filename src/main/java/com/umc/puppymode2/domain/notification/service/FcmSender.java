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
public class FcmSender {

    private final FcmTokenRepository fcmTokenRepository;

    private static final int FCM_BATCH_SIZE = 500;

    /**
     * 유저별 개인화 메시지 전송
     * sendEach 사용 → 메시지마다 내용이 달라도 한 번의 HTTP 요청으로 처리
     */
    @Transactional
    public void sendPersonalized(List<DrinkReminderTarget> targets) {
        if (targets.isEmpty()) return;

        log.info("[FCM] 전송 시작 - 총 {}명", targets.size());

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

                if (response.getFailureCount() > 0) {
                    handleFailures(batch, response);
                }
            } catch (FirebaseMessagingException e) {
                log.error("[FCM] 배치 전송 실패", e);
            }
        }
    }

    private void handleFailures(List<DrinkReminderTarget> targets, BatchResponse response) {
        List<SendResponse> responses = response.getResponses();
        for (int i = 0; i < responses.size(); i++) {
            if (!responses.get(i).isSuccessful()) {
                MessagingErrorCode code = responses.get(i).getException().getMessagingErrorCode();
                log.warn("[FCM] 토큰 전송 실패 - user: {}, errorCode: {}",
                        targets.get(i).username(), code);
                if (code == MessagingErrorCode.UNREGISTERED
                        || code == MessagingErrorCode.INVALID_ARGUMENT) {
                    log.warn("[FCM] 만료 토큰 삭제 - user: {}", targets.get(i).username());
                    fcmTokenRepository.deleteByFcmToken(targets.get(i).fcmToken());
                }
            }
        }
    }

    private <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
    }
}