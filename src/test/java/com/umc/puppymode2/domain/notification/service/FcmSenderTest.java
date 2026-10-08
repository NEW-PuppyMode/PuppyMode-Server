package com.umc.puppymode2.domain.notification.service;

import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.umc.puppymode2.domain.notification.repository.FcmTokenRepository;
import com.umc.puppymode2.global.metrics.NotificationMetrics;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** FCM 호출을 FcmClient로 분리한 뒤에도 FcmSender의 결과 집계와 만료 토큰 처리가 그대로인지 확인한다. */
class FcmSenderTest {

    private final FcmTokenRepository tokenRepository = mock(FcmTokenRepository.class);
    private final FcmClient client = mock(FcmClient.class);
    private final FcmSender sender = new FcmSender(tokenRepository, client, NotificationMetrics.noop());

    private static FcmClient.Item ok() {
        return new FcmClient.Item(true, "id", null, null);
    }

    private static FcmClient.Item fail(MessagingErrorCode code) {
        return new FcmClient.Item(false, null, code, "msg");
    }

    @Test
    void 성공과_실패_건수를_그대로_돌려준다() throws Exception {
        when(client.sendEach(any())).thenReturn(new FcmClient.BatchResult(2, 0, List.of(ok(), ok())));

        FcmSender.FcmSendResult result = sender.sendToTokens(List.of("t1", "t2"), "제목", "본문", "friend_list");

        assertEquals(2, result.successCount());
        assertEquals(0, result.failureCount());
        verify(tokenRepository, never()).deleteByFcmToken(any());
    }

    @Test
    void 만료_토큰만_삭제하고_일시_오류_토큰은_남긴다() throws Exception {
        when(client.sendEach(any())).thenReturn(new FcmClient.BatchResult(1, 3,
                List.of(ok(), fail(MessagingErrorCode.UNREGISTERED), fail(MessagingErrorCode.INVALID_ARGUMENT), fail(MessagingErrorCode.UNAVAILABLE))));

        FcmSender.FcmSendResult result = sender.sendToTokens(List.of("t1", "t2", "t3", "t4"), "제목", "본문", "friend_list");

        assertEquals(1, result.successCount());
        assertEquals(3, result.failureCount());
        verify(tokenRepository).deleteByFcmToken("t2");
        verify(tokenRepository).deleteByFcmToken("t3");
        verify(tokenRepository, never()).deleteByFcmToken("t4");
        verify(tokenRepository, never()).deleteByFcmToken("t1");
    }

    @Test
    void FirebaseMessagingException이면_전부_실패로_센다() throws Exception {
        when(client.sendEach(any())).thenThrow(mock(FirebaseMessagingException.class));

        FcmSender.FcmSendResult result = sender.sendToTokens(List.of("t1", "t2"), "제목", "본문", "friend_list");

        assertEquals(0, result.successCount());
        assertEquals(2, result.failureCount());
    }

    @Test
    void Firebase가_초기화되지_않아_IllegalStateException이_나면_그대로_밖으로_전파된다() throws Exception {
        // 기존 동작: FirebaseMessagingException만 잡는다. 교체 지점을 만들어도 이 동작은 바뀌지 않는다.
        when(client.sendEach(any())).thenThrow(new IllegalStateException("FirebaseApp with name [DEFAULT] doesn't exist."));

        assertThrows(IllegalStateException.class, () -> sender.sendToTokens(List.of("t1"), "제목", "본문", "friend_list"));
    }

    @Test
    void 토큰이_없으면_FCM을_호출하지_않는다() throws Exception {
        sender.sendToTokens(List.of(), "제목", "본문", "friend_list");

        verifyNoInteractions(client);
    }
}
