package com.umc.puppymode2.domain.notification.service;

import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;

import java.util.List;

/**
 * FCM 호출 지점. 운영에서는 {@link FirebaseFcmClient}가 Firebase를 그대로 호출한다.
 * 로컬 부하 테스트에서만 {@link FakeFcmClient}로 바뀐다. (loadtest.fake-fcm.enabled=true)
 */
public interface FcmClient {

    /** 메시지마다 한 번씩 보낸다. (FirebaseMessaging.sendEach와 같은 의미) */
    BatchResult sendEach(List<Message> messages) throws FirebaseMessagingException;

    record BatchResult(int successCount, int failureCount, List<Item> items) {}

    /** 메시지 하나의 결과. 실패하면 errorCode와 errorMessage가 채워진다. */
    record Item(boolean successful, String messageId, MessagingErrorCode errorCode, String errorMessage) {}
}
