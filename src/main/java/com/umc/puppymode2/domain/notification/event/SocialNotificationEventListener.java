package com.umc.puppymode2.domain.notification.event;

import com.umc.puppymode2.domain.cheer.event.CheerReceivedEvent;
import com.umc.puppymode2.domain.friend.event.FriendRequestAcceptedEvent;
import com.umc.puppymode2.domain.friend.event.FriendRequestReceivedEvent;
import com.umc.puppymode2.domain.notification.entity.FcmToken;
import com.umc.puppymode2.domain.notification.repository.FcmTokenRepository;
import com.umc.puppymode2.domain.notification.service.FcmSender;
import com.umc.puppymode2.domain.user.entity.User;
import com.umc.puppymode2.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SocialNotificationEventListener {

    private final UserRepository userRepository;
    private final FcmTokenRepository fcmTokenRepository;
    private final FcmSender fcmSender;

    // TODO: 푸시 문구 PM 확정 필요 (#194)
    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFriendRequestReceived(FriendRequestReceivedEvent event) {
        notify(event.receiverId(), event.requesterId(),
                "친구 요청이 도착했어요", "님이 친구가 되고 싶어해요", "friend_request");
    }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFriendRequestAccepted(FriendRequestAcceptedEvent event) {
        notify(event.receiverId(), event.accepterId(),
                "친구 요청이 수락됐어요", "님과 친구가 됐어요", "friend_list");
    }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCheerReceived(CheerReceivedEvent event) {
        notify(event.receiverId(), event.senderId(),
                "응원이 도착했어요", "님이 응원을 보냈어요", "cheers_received");
    }

    private void notify(Long receiverId, Long actorId, String title, String bodySuffix, String landing) {
        User receiver = userRepository.findById(receiverId).orElse(null);
        if (receiver == null || !receiver.isReceiveFriendNotifications()) {
            return;
        }

        List<String> tokens = fcmTokenRepository.findByUserUserId(receiverId).stream()
                .map(FcmToken::getFcmToken)
                .toList();
        if (tokens.isEmpty()) {
            return;
        }

        String actorName = userRepository.findById(actorId)
                .map(User::getUsername)
                .orElse("친구");

        fcmSender.sendToTokens(tokens, title, actorName + bodySuffix, landing);
    }
}
