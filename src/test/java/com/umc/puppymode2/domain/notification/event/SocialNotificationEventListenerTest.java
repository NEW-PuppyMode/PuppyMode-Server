package com.umc.puppymode2.domain.notification.event;

import com.umc.puppymode2.domain.cheer.event.CheerReceivedEvent;
import com.umc.puppymode2.domain.friend.event.FriendRequestAcceptedEvent;
import com.umc.puppymode2.domain.friend.event.FriendRequestReceivedEvent;
import com.umc.puppymode2.domain.notification.entity.FcmToken;
import com.umc.puppymode2.domain.notification.repository.FcmTokenRepository;
import com.umc.puppymode2.domain.notification.service.FcmSender;
import com.umc.puppymode2.domain.user.auth.enums.Provider;
import com.umc.puppymode2.domain.user.entity.User;
import com.umc.puppymode2.domain.user.entity.enums.UserStatus;
import com.umc.puppymode2.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SocialNotificationEventListenerTest {

    @Mock private UserRepository userRepository;
    @Mock private FcmTokenRepository fcmTokenRepository;
    @Mock private FcmSender fcmSender;

    @InjectMocks
    private SocialNotificationEventListener listener;

    private static final Long RECEIVER = 10L;
    private static final Long REQUESTER = 20L;

    private User user(Long userId, String username, boolean receiveFriendNotifications) {
        User user = User.builder()
                .username(username)
                .email(userId + "@test.com")
                .provider(Provider.KAKAO)
                .status(UserStatus.NORMAL)
                .build();
        ReflectionTestUtils.setField(user, "userId", userId);
        ReflectionTestUtils.setField(user, "receiveFriendNotifications", receiveFriendNotifications);
        return user;
    }

    @Test
    void 친구_알림이_꺼져있으면_발송하지_않는다() {
        when(userRepository.findById(RECEIVER)).thenReturn(Optional.of(user(RECEIVER, "receiver", false)));

        listener.onFriendRequestReceived(new FriendRequestReceivedEvent(RECEIVER, REQUESTER));

        verifyNoInteractions(fcmTokenRepository);
        verifyNoInteractions(fcmSender);
    }

    @Test
    void 수신자가_존재하지_않으면_발송하지_않는다() {
        when(userRepository.findById(RECEIVER)).thenReturn(Optional.empty());

        listener.onFriendRequestReceived(new FriendRequestReceivedEvent(RECEIVER, REQUESTER));

        verifyNoInteractions(fcmTokenRepository);
        verifyNoInteractions(fcmSender);
    }

    @Test
    void 등록된_토큰이_없으면_발송하지_않는다() {
        when(userRepository.findById(RECEIVER)).thenReturn(Optional.of(user(RECEIVER, "receiver", true)));
        when(fcmTokenRepository.findByUserUserId(RECEIVER)).thenReturn(List.of());

        listener.onFriendRequestReceived(new FriendRequestReceivedEvent(RECEIVER, REQUESTER));

        verifyNoInteractions(fcmSender);
    }

    @Test
    @SuppressWarnings("unchecked")
    void 설정이_켜져있고_토큰이_있으면_요청자_이름을_담아_발송한다() {
        User receiver = user(RECEIVER, "receiver", true);
        User requester = user(REQUESTER, "쿠키", true);
        when(userRepository.findById(RECEIVER)).thenReturn(Optional.of(receiver));
        when(userRepository.findById(REQUESTER)).thenReturn(Optional.of(requester));
        when(fcmTokenRepository.findByUserUserId(RECEIVER)).thenReturn(List.of(
                new FcmToken(receiver, "token-1"),
                new FcmToken(receiver, "token-2")
        ));

        listener.onFriendRequestReceived(new FriendRequestReceivedEvent(RECEIVER, REQUESTER));

        ArgumentCaptor<List<String>> tokensCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(fcmSender).sendToTokens(tokensCaptor.capture(), anyString(), bodyCaptor.capture(), anyString());

        assertEquals(List.of("token-1", "token-2"), tokensCaptor.getValue());
        assertTrue(bodyCaptor.getValue().contains("쿠키"));
    }

    @Test
    void 요청자_정보를_찾지_못해도_기본_문구로_발송한다() {
        User receiver = user(RECEIVER, "receiver", true);
        when(userRepository.findById(RECEIVER)).thenReturn(Optional.of(receiver));
        when(userRepository.findById(REQUESTER)).thenReturn(Optional.empty());
        when(fcmTokenRepository.findByUserUserId(RECEIVER)).thenReturn(List.of(new FcmToken(receiver, "token-1")));

        listener.onFriendRequestReceived(new FriendRequestReceivedEvent(RECEIVER, REQUESTER));

        verify(fcmSender).sendToTokens(eq(List.of("token-1")), anyString(), anyString(), anyString());
    }

    @Test
    void 친구_요청_수락_시_요청자에게_수락자_이름을_담아_발송한다() {
        User requester = user(RECEIVER, "requester", true);
        User accepter = user(REQUESTER, "쿠키", true);
        when(userRepository.findById(RECEIVER)).thenReturn(Optional.of(requester));
        when(userRepository.findById(REQUESTER)).thenReturn(Optional.of(accepter));
        when(fcmTokenRepository.findByUserUserId(RECEIVER)).thenReturn(List.of(new FcmToken(requester, "token-1")));

        listener.onFriendRequestAccepted(new FriendRequestAcceptedEvent(RECEIVER, REQUESTER));

        verify(fcmSender).sendToTokens(
                List.of("token-1"), "친구 요청이 수락됐어요", "쿠키님과 친구가 됐어요", "friend_list");
    }

    @Test
    void 응원을_받으면_발신자_이름을_담아_발송한다() {
        User receiver = user(RECEIVER, "receiver", true);
        User sender = user(REQUESTER, "쿠키", true);
        when(userRepository.findById(RECEIVER)).thenReturn(Optional.of(receiver));
        when(userRepository.findById(REQUESTER)).thenReturn(Optional.of(sender));
        when(fcmTokenRepository.findByUserUserId(RECEIVER)).thenReturn(List.of(new FcmToken(receiver, "token-1")));

        listener.onCheerReceived(new CheerReceivedEvent(RECEIVER, REQUESTER));

        verify(fcmSender).sendToTokens(
                List.of("token-1"), "응원이 도착했어요", "쿠키님이 응원을 보냈어요", "cheers_received");
    }

    @Test
    void 응원을_받아도_친구_알림이_꺼져있으면_발송하지_않는다() {
        when(userRepository.findById(RECEIVER)).thenReturn(Optional.of(user(RECEIVER, "receiver", false)));

        listener.onCheerReceived(new CheerReceivedEvent(RECEIVER, REQUESTER));

        verifyNoInteractions(fcmSender);
    }
}
