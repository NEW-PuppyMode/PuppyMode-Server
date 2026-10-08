package com.umc.puppymode2.domain.notification.service;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.SendResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 실제 FCM 호출. 기존 FcmSender가 하던 {@code FirebaseMessaging.getInstance().sendEach(...)} 호출을 그대로 옮겼다.
 * loadtest.fake-fcm.enabled가 true가 아니면 항상 이 구현이 쓰인다.
 */
@Component
@ConditionalOnProperty(name = "loadtest.fake-fcm.enabled", havingValue = "false", matchIfMissing = true)
public class FirebaseFcmClient implements FcmClient {

    @Override
    public BatchResult sendEach(List<Message> messages) throws FirebaseMessagingException {
        BatchResponse response = FirebaseMessaging.getInstance().sendEach(messages);

        List<Item> items = new ArrayList<>(response.getResponses().size());
        for (SendResponse sendResponse : response.getResponses()) {
            if (sendResponse.isSuccessful()) {
                items.add(new Item(true, sendResponse.getMessageId(), null, null));
            } else {
                FirebaseMessagingException ex = sendResponse.getException();
                items.add(new Item(false, null, ex.getMessagingErrorCode(), ex.getMessage()));
            }
        }
        return new BatchResult(response.getSuccessCount(), response.getFailureCount(), items);
    }
}
