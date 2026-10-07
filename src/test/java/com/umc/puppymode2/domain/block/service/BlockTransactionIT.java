package com.umc.puppymode2.domain.block.service;

import com.umc.puppymode2.domain.block.dto.BlockListResponseDTO;
import com.umc.puppymode2.domain.block.exception.BlockErrorStatus;
import com.umc.puppymode2.domain.block.repository.UserBlockRepository;
import com.umc.puppymode2.domain.cheer.entity.Cheer;
import com.umc.puppymode2.domain.cheer.entity.CheerTemplate;
import com.umc.puppymode2.domain.cheer.entity.enums.CheerCategory;
import com.umc.puppymode2.domain.cheer.repository.CheerRepository;
import com.umc.puppymode2.domain.cheer.repository.CheerTemplateRepository;
import com.umc.puppymode2.domain.friend.entity.FriendCode;
import com.umc.puppymode2.domain.friend.entity.FriendRequest;
import com.umc.puppymode2.domain.friend.entity.Friendship;
import com.umc.puppymode2.domain.friend.entity.enums.FriendRequestStatus;
import com.umc.puppymode2.domain.friend.exception.FriendErrorStatus;
import com.umc.puppymode2.domain.friend.repository.FriendCodeRepository;
import com.umc.puppymode2.domain.friend.repository.FriendRequestRepository;
import com.umc.puppymode2.domain.friend.repository.FriendshipRepository;
import com.umc.puppymode2.domain.friend.service.FriendCommandService;
import com.umc.puppymode2.domain.notification.service.FcmSender;
import com.umc.puppymode2.domain.user.auth.enums.Provider;
import com.umc.puppymode2.domain.user.entity.User;
import com.umc.puppymode2.domain.user.entity.enums.UserStatus;
import com.umc.puppymode2.domain.user.repository.UserRepository;
import com.umc.puppymode2.global.apiPayload.code.BaseErrorCode;
import com.umc.puppymode2.global.exception.GeneralException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.spec.ECGenParameterSpec;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 차단의 트랜잭션 동작을 실제 MySQL·Redis 컨테이너로 검증한다. (mock으로는 롤백·UNIQUE·동시성을 검증할 수 없다)
 */
@Testcontainers
@SpringBootTest
class BlockTransactionIT {

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
        registry.add("auth.jwt.secret", BlockTransactionIT::randomJwtSecret);
        registry.add("auth.kakao.rest-api-key", () -> "test");
        registry.add("auth.kakao.redirect-uri", () -> "http://localhost/callback");
        registry.add("auth.apple.team-id", () -> "test");
        registry.add("auth.apple.client-id", () -> "test");
        registry.add("auth.apple.key-id", () -> "test");
        registry.add("auth.apple.private-key-lines[0]", BlockTransactionIT::testEcPrivateKeyBase64);
        registry.add("auth.apple.redirect-uri", () -> "https://localhost/callback");
    }

    @MockitoBean
    private FcmSender fcmSender;

    @Autowired private BlockCommandService blockCommandService;
    @Autowired private BlockQueryService blockQueryService;
    @Autowired private FriendCommandService friendCommandService;
    @Autowired private UserRepository userRepository;
    @Autowired private UserBlockRepository userBlockRepository;
    @Autowired private FriendshipRepository friendshipRepository;
    @Autowired private FriendRequestRepository friendRequestRepository;
    @Autowired private FriendCodeRepository friendCodeRepository;
    @Autowired private CheerRepository cheerRepository;
    @Autowired private CheerTemplateRepository cheerTemplateRepository;

    private Long me;
    private Long other;
    private Long third;

    @BeforeEach
    void setUp() {
        me = saveUser("me").getUserId();
        other = saveUser("other").getUserId();
        third = saveUser("third").getUserId();
    }

    @AfterEach
    void tearDown() {
        cheerRepository.deleteAll();
        friendRequestRepository.deleteAll();
        friendshipRepository.deleteAll();
        userBlockRepository.deleteAll();
        friendCodeRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void 차단하면_친구_응원_대기_요청이_한번에_지워지고_다른_친구는_그대로다() {
        friendshipRepository.save(Friendship.of(me, other));
        friendshipRepository.save(Friendship.of(me, third));

        CheerTemplate template = anyTemplate();
        LocalDate today = LocalDate.now();
        cheerRepository.save(Cheer.of(me, other, template, today, LocalDateTime.now().plusDays(1)));
        cheerRepository.save(Cheer.of(other, me, template, today, LocalDateTime.now().minusDays(1))); // 이미 만료된 응원도 지운다
        cheerRepository.save(Cheer.of(me, third, template, today, LocalDateTime.now().plusDays(1)));

        friendRequestRepository.save(FriendRequest.create(other, me));      // 대기 중 → 지운다
        FriendRequest accepted = FriendRequest.create(me, other);
        accepted.accept();
        friendRequestRepository.save(accepted);                              // 수락된 이력 → 남긴다

        blockCommandService.block(me, other);

        assertTrue(userBlockRepository.existsByBlockerIdAndBlockedId(me, other));
        assertFalse(friendshipRepository.existsByUserLowIdAndUserHighId(Math.min(me, other), Math.max(me, other)));
        assertTrue(friendshipRepository.existsByUserLowIdAndUserHighId(Math.min(me, third), Math.max(me, third)));

        assertTrue(cheerRepository.existsBySenderIdAndReceiverIdAndTargetDate(me, third, today));
        assertFalse(cheerRepository.existsBySenderIdAndReceiverIdAndTargetDate(me, other, today));
        assertFalse(cheerRepository.existsBySenderIdAndReceiverIdAndTargetDate(other, me, today));

        assertTrue(friendRequestRepository.findByRequesterIdAndReceiverId(other, me).isEmpty());
        assertEquals(FriendRequestStatus.ACCEPTED,
                friendRequestRepository.findByRequesterIdAndReceiverId(me, other).orElseThrow().getStatus());

        BlockListResponseDTO list = blockQueryService.getBlocks(me);
        assertEquals(1, list.getCount());
        assertEquals(other, list.getBlocks().get(0).getUserId());
    }

    @Test
    void 친구가_아니면_403이고_차단_기록도_남지_않는다() {
        GeneralException e = assertThrows(GeneralException.class, () -> blockCommandService.block(me, other));

        assertEquals(BlockErrorStatus.NOT_FRIENDS, e.getCode());
        // 차단 기록을 먼저 저장한 뒤 친구 확인에서 예외가 나므로, 롤백되지 않으면 기록이 남는다.
        assertFalse(userBlockRepository.existsByBlockerIdAndBlockedId(me, other));
        assertEquals(0, userBlockRepository.count());
    }

    @Test
    void 같은_차단_요청이_동시에_들어오면_하나만_성공하고_나머지는_409다() throws Exception {
        friendshipRepository.save(Friendship.of(me, other));
        friendRequestRepository.save(FriendRequest.create(other, me));

        int threads = 4;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<BaseErrorCode>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            Callable<BaseErrorCode> task = () -> {
                ready.countDown();
                go.await();
                try {
                    blockCommandService.block(me, other);
                    return null; // 성공
                } catch (GeneralException e) {
                    return e.getCode();
                }
            };
            futures.add(pool.submit(task));
        }
        ready.await();
        go.countDown();

        int success = 0;
        int alreadyBlocked = 0;
        for (Future<BaseErrorCode> future : futures) {
            BaseErrorCode result = future.get();
            if (result == null) {
                success++;
            } else if (result == BlockErrorStatus.ALREADY_BLOCKED) {
                alreadyBlocked++;
            } else {
                fail("예상하지 못한 결과: " + result);
            }
        }
        pool.shutdown();

        assertEquals(1, success);
        assertEquals(threads - 1, alreadyBlocked);
        assertEquals(1, userBlockRepository.count());
        assertFalse(friendshipRepository.existsByUserLowIdAndUserHighId(Math.min(me, other), Math.max(me, other)));
        assertTrue(friendRequestRepository.findByRequesterIdAndReceiverId(other, me).isEmpty());
    }

    @Test
    void 서로_동시에_차단해도_하나만_성공하고_나머지는_친구_아님으로_정리된다() throws Exception {
        // A→B, B→A는 방향별 UNIQUE에 걸리지 않아 둘 다 차단 기록 저장을 통과한다. 이어서 같은 친구 관계를 지우려 할 때
        // 한쪽이 0행을 지우게 되므로, 예외가 아니라 "친구 아님"(403)으로 정리돼야 한다. 경쟁이 매번 일어나진 않아서 반복한다.
        for (int round = 0; round < 15; round++) {
            friendshipRepository.save(Friendship.of(me, other));

            ExecutorService pool = Executors.newFixedThreadPool(2);
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch go = new CountDownLatch(1);
            List<Callable<Object>> tasks = List.of(
                    () -> runBlock(ready, go, me, other),
                    () -> runBlock(ready, go, other, me));
            List<Future<Object>> futures = new ArrayList<>();
            for (Callable<Object> task : tasks) {
                futures.add(pool.submit(task));
            }
            ready.await();
            go.countDown();

            int success = 0;
            for (Future<Object> future : futures) {
                Object result = future.get();
                if (result == null) {
                    success++;
                } else {
                    assertEquals(BlockErrorStatus.NOT_FRIENDS, result, "라운드 " + round + ": 예상하지 못한 결과 " + result);
                }
            }
            pool.shutdown();

            assertEquals(1, success, "라운드 " + round);
            assertEquals(1, userBlockRepository.count(), "라운드 " + round);
            assertFalse(friendshipRepository.existsByUserLowIdAndUserHighId(Math.min(me, other), Math.max(me, other)));
            userBlockRepository.deleteAll();
        }
    }

    // 성공이면 null, 실패면 GeneralException의 코드 또는 예외 자체를 돌려준다.
    private Object runBlock(CountDownLatch ready, CountDownLatch go, Long blocker, Long blocked) throws InterruptedException {
        ready.countDown();
        go.await();
        try {
            blockCommandService.block(blocker, blocked);
            return null;
        } catch (GeneralException e) {
            return e.getCode();
        } catch (Exception e) {
            return e;
        }
    }

    @Test
    void 차단_중에는_양쪽_모두_친구_요청이_없는_코드로_막히고_해제하면_다시_보낼_수_있다() {
        friendCodeRepository.save(FriendCode.of(me, "1111"));
        friendCodeRepository.save(FriendCode.of(other, "2222"));
        friendshipRepository.save(Friendship.of(me, other));

        blockCommandService.block(me, other);

        // 내가 차단했으므로 내가 상대 코드를 입력해도, 차단당한 상대가 내 코드를 입력해도 같은 응답이다.
        assertEquals(FriendErrorStatus.FRIEND_CODE_NOT_FOUND,
                assertThrows(GeneralException.class, () -> friendCommandService.sendFriendRequest(me, "2222")).getCode());
        assertEquals(FriendErrorStatus.FRIEND_CODE_NOT_FOUND,
                assertThrows(GeneralException.class, () -> friendCommandService.sendFriendRequest(other, "1111")).getCode());

        blockCommandService.unblock(me, other);

        // 해제해도 친구는 복구되지 않는다.
        assertFalse(friendshipRepository.existsByUserLowIdAndUserHighId(Math.min(me, other), Math.max(me, other)));
        // 대신 친구 요청을 새로 보낼 수 있다.
        assertEquals(FriendRequestStatus.PENDING, friendCommandService.sendFriendRequest(other, "1111").getStatus());
    }

    @Test
    void 차단한_기록이_없으면_해제는_404다() {
        GeneralException e = assertThrows(GeneralException.class, () -> blockCommandService.unblock(me, other));

        assertEquals(BlockErrorStatus.BLOCK_NOT_FOUND, e.getCode());
    }

    private CheerTemplate anyTemplate() {
        return cheerTemplateRepository.findAll().stream().findFirst()
                .orElseGet(() -> cheerTemplateRepository.save(CheerTemplate.of(CheerCategory.values()[0], "테스트 응원", 1)));
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
