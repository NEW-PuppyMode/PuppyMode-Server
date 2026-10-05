package com.umc.puppymode2.domain.notification.event;

import com.umc.puppymode2.domain.friend.entity.FriendCode;
import com.umc.puppymode2.domain.friend.event.FriendRequestReceivedEvent;
import com.umc.puppymode2.domain.friend.repository.FriendCodeRepository;
import com.umc.puppymode2.domain.friend.repository.FriendRequestRepository;
import com.umc.puppymode2.domain.friend.service.FriendCommandService;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@Testcontainers
@SpringBootTest
class SocialNotificationTransactionIT {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0");

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.jpa.properties.hibernate.hbm2ddl.auto", () -> "create-drop");
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("auth.jwt.secret", SocialNotificationTransactionIT::randomJwtSecret);
        registry.add("auth.kakao.rest-api-key", () -> "test");
        registry.add("auth.kakao.redirect-uri", () -> "http://localhost/callback");
        registry.add("auth.apple.team-id", () -> "test");
        registry.add("auth.apple.client-id", () -> "test");
        registry.add("auth.apple.key-id", () -> "test");
        registry.add("auth.apple.private-key-lines[0]", SocialNotificationTransactionIT::testEcPrivateKeyBase64);
        registry.add("auth.apple.redirect-uri", () -> "https://localhost/callback");
    }

    @MockitoBean
    private FcmSender fcmSender;

    @Autowired private FriendCommandService friendCommandService;
    @Autowired private ApplicationEventPublisher eventPublisher;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private UserRepository userRepository;
    @Autowired private FcmTokenRepository fcmTokenRepository;
    @Autowired private FriendCodeRepository friendCodeRepository;
    @Autowired private FriendRequestRepository friendRequestRepository;

    private Long receiverId;
    private Long requesterId;

    @BeforeEach
    void setUp() {
        receiverId = saveUser("receiver").getUserId();
        requesterId = saveUser("requester").getUserId();
        friendCodeRepository.save(FriendCode.of(receiverId, "1234"));
        User receiver = userRepository.findById(receiverId).orElseThrow();
        fcmTokenRepository.save(new FcmToken(receiver, "token-1"));
    }

    @AfterEach
    void tearDown() {
        friendRequestRepository.deleteAll();
        fcmTokenRepository.deleteAll();
        friendCodeRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void 친구_요청이_커밋되면_수신자에게_발송된다() {
        friendCommandService.sendFriendRequest(requesterId, "1234");

        verify(fcmSender, timeout(5000)).sendToTokens(
                eq(List.of("token-1")), anyString(), anyString(), eq("friend_request"));
    }

    @Test
    void 트랜잭션이_롤백되면_이벤트가_있어도_발송되지_않는다() {
        new TransactionTemplate(transactionManager).execute(status -> {
            eventPublisher.publishEvent(new FriendRequestReceivedEvent(receiverId, requesterId));
            status.setRollbackOnly();
            return null;
        });

        verify(fcmSender, after(1500).never()).sendToTokens(any(), any(), any(), any());
    }

    @Test
    void 수신자가_친구_알림을_끄면_커밋돼도_발송되지_않는다() {
        User receiver = userRepository.findById(receiverId).orElseThrow();
        receiver.updateFriendNotificationSetting(false);
        userRepository.saveAndFlush(receiver);

        friendCommandService.sendFriendRequest(requesterId, "1234");

        verify(fcmSender, after(1500).never()).sendToTokens(any(), any(), any(), any());
    }

    private User saveUser(String name) {
        return userRepository.save(User.builder()
                .username(name)
                .email(name + "@test.com")
                .provider(Provider.KAKAO)
                .status(UserStatus.NORMAL)
                .build());
    }

    private static String randomJwtSecret() {
        byte[] bytes = new byte[48];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().encodeToString(bytes);
    }

    private static String testEcPrivateKeyBase64() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(new ECGenParameterSpec("secp256r1"));
            KeyPair pair = generator.generateKeyPair();
            return Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
