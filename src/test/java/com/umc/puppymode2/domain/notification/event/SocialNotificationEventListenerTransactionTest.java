package com.umc.puppymode2.domain.notification.event;

import com.umc.puppymode2.domain.friend.event.FriendRequestReceivedEvent;
import com.umc.puppymode2.domain.notification.entity.FcmToken;
import com.umc.puppymode2.domain.notification.repository.FcmTokenRepository;
import com.umc.puppymode2.domain.notification.service.FcmSender;
import com.umc.puppymode2.domain.user.auth.enums.Provider;
import com.umc.puppymode2.domain.user.entity.User;
import com.umc.puppymode2.domain.user.entity.enums.UserStatus;
import com.umc.puppymode2.domain.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.event.TransactionalEventListenerFactory;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringJUnitConfig(SocialNotificationEventListenerTransactionTest.TestConfig.class)
class SocialNotificationEventListenerTransactionTest {

    @Autowired private ApplicationEventPublisher eventPublisher;
    @Autowired private UserRepository userRepository;
    @Autowired private FcmTokenRepository fcmTokenRepository;
    @Autowired private FcmSender fcmSender;

    private static final Long RECEIVER = 10L;
    private static final Long REQUESTER = 20L;

    @BeforeEach
    void setUp() {
        reset(userRepository, fcmTokenRepository, fcmSender);
        User receiver = user(RECEIVER, "receiver");
        User requester = user(REQUESTER, "쿠키");
        when(userRepository.findById(RECEIVER)).thenReturn(Optional.of(receiver));
        when(userRepository.findById(REQUESTER)).thenReturn(Optional.of(requester));
        when(fcmTokenRepository.findByUserUserId(RECEIVER)).thenReturn(List.of(new FcmToken(receiver, "token-1")));
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    private void commit() {
        TransactionSynchronizationUtils.triggerAfterCommit();
        TransactionSynchronizationUtils.triggerAfterCompletion(TransactionSynchronization.STATUS_COMMITTED);
    }

    private void beginTransaction() {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
    }

    @Test
    void 트랜잭션이_커밋되기_전에는_발송하지_않는다() {
        beginTransaction();

        eventPublisher.publishEvent(new FriendRequestReceivedEvent(RECEIVER, REQUESTER));

        verifyNoInteractions(fcmSender);
    }

    @Test
    void 트랜잭션이_커밋되면_그때_발송한다() {
        beginTransaction();
        eventPublisher.publishEvent(new FriendRequestReceivedEvent(RECEIVER, REQUESTER));

        commit();

        verify(fcmSender, times(1)).sendToTokens(eq(List.of("token-1")), anyString(), anyString(), eq("friend_request"));
    }

    @Test
    void 트랜잭션이_롤백되면_발송하지_않는다() {
        beginTransaction();
        eventPublisher.publishEvent(new FriendRequestReceivedEvent(RECEIVER, REQUESTER));

        TransactionSynchronizationUtils.triggerAfterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

        verifyNoInteractions(fcmSender);
    }

    @Test
    void 활성_트랜잭션이_없으면_이벤트를_발송하지_않고_버린다() {
        assertFalse(TransactionSynchronizationManager.isSynchronizationActive());

        eventPublisher.publishEvent(new FriendRequestReceivedEvent(RECEIVER, REQUESTER));

        verifyNoInteractions(fcmSender);
    }

    private User user(Long userId, String username) {
        User user = User.builder()
                .username(username)
                .email(userId + "@test.com")
                .provider(Provider.KAKAO)
                .status(UserStatus.NORMAL)
                .build();
        ReflectionTestUtils.setField(user, "userId", userId);
        ReflectionTestUtils.setField(user, "receiveFriendNotifications", true);
        return user;
    }

    @Configuration
    @EnableAsync
    static class TestConfig {

        @Bean(name = "notificationExecutor")
        Executor notificationExecutor() {
            return new SyncTaskExecutor();
        }

        @Bean
        TransactionalEventListenerFactory transactionalEventListenerFactory() {
            return new TransactionalEventListenerFactory();
        }

        @Bean
        UserRepository userRepository() {
            return mock(UserRepository.class);
        }

        @Bean
        FcmTokenRepository fcmTokenRepository() {
            return mock(FcmTokenRepository.class);
        }

        @Bean
        FcmSender fcmSender() {
            return mock(FcmSender.class);
        }

        @Bean
        SocialNotificationEventListener socialNotificationEventListener(UserRepository userRepository,
                                                                        FcmTokenRepository fcmTokenRepository,
                                                                        FcmSender fcmSender) {
            return new SocialNotificationEventListener(userRepository, fcmTokenRepository, fcmSender);
        }
    }
}
