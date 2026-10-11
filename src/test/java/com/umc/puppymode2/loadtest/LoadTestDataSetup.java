package com.umc.puppymode2.loadtest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.puppymode2.domain.friend.entity.Friendship;
import com.umc.puppymode2.domain.user.auth.enums.Provider;
import com.umc.puppymode2.domain.user.entity.User;
import com.umc.puppymode2.domain.user.entity.enums.UserStatus;
import com.umc.puppymode2.domain.user.repository.UserRepository;
import com.umc.puppymode2.global.auth.token.JwtTokenProvider;
import com.umc.puppymode2.global.util.TimeConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 로컬 부하 테스트(#214, #216)용 테스트 유저와 JWT, 소셜 데이터를 만들고 정리하는 도구.
 *
 * 평소 {@code ./gradlew test}에서는 환경 변수 LOADTEST_MODE가 없어 모두 건너뛴다.
 * 직접 실행할 때는 scripts/perf/loadtest-data.sh 를 사용한다.
 *
 *   setup   : 테스트 유저를 멱등하게 만들고 scripts/perf/out/tokens.json 에 토큰을 쓴다.
 *             푸시 경로(#216)를 위해 테스트 유저끼리 모두 친구로 만들고, 오늘·어제 음주 기록과 가짜 FCM 토큰(유저당 2개)을 만든다.
 *   reset   : 테스트 유저가 관련된 신고·응원 데이터만 지운다 (UNIQUE 충돌 없이 시나리오를 다시 돌리기 위함)
 *   cleanup : reset + 테스트 유저 관련 친구 관계·친구 요청·차단·FCM 토큰·음주 기록, 테스트 유저와 토큰 파일 삭제
 *
 * 안전장치
 *   - 활성 DB가 localhost가 아니면 즉시 중단한다. (운영/개발 DB를 건드리지 않기 위해)
 *   - 이메일이 loadtest{N}@puppymode.local 형식인 유저만 대상으로 한다. 실제 유저와 기존 더미(dummy*)는 건드리지 않는다.
 *   - 토큰은 앱과 같은 JwtTokenProvider로 서명하며, 파일에만 쓰고 로그에는 남기지 않는다.
 */
@SpringBootTest
class LoadTestDataSetup {

    private static final String EMAIL_PREFIX = "loadtest";
    private static final String EMAIL_DOMAIN = "@puppymode.local";
    private static final String EMAIL_LIKE = EMAIL_PREFIX + "%" + EMAIL_DOMAIN;
    private static final Path TOKEN_FILE = Path.of("scripts", "perf", "out", "tokens.json");
    private static final Duration TOKEN_TTL = Duration.ofHours(24);
    private static final int FCM_TOKENS_PER_USER = 2;

    private static final String LOADTEST_USER_IDS = "SELECT user_id FROM `user` WHERE email LIKE ?";

    @Autowired private UserRepository userRepository;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ObjectMapper objectMapper;

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @BeforeEach
    void requireLocalDatabase() {
        assertTrue(isLocalHost(datasourceUrl),
                "부하 테스트 도구는 localhost DB에서만 실행할 수 있습니다. (활성 DB 호스트가 localhost가 아님)");
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "LOADTEST_MODE", matches = "setup")
    void setup() throws IOException {
        int count = userCount();

        for (int i = 1; i <= count; i++) {
            String email = EMAIL_PREFIX + i + EMAIL_DOMAIN;
            if (userRepository.findByEmail(email).isEmpty()) {
                userRepository.save(User.builder()
                        .username("loadtest-user-" + i)
                        .email(email)
                        .provider(Provider.KAKAO)
                        .receiveNotifications(false)
                        .status(UserStatus.NORMAL)
                        .isCustomName(false)
                        .build());
            }
        }

        List<Map<String, Object>> tokens = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            User user = userRepository.findByEmail(EMAIL_PREFIX + i + EMAIL_DOMAIN).orElseThrow();
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("userId", user.getUserId());
            entry.put("token", jwtTokenProvider.generateToken(user.getUserId(), TOKEN_TTL, "ACCESS"));
            tokens.add(entry);
        }

        // 푸시 경로(응원)에 필요한 데이터. 날짜는 앱과 같은 KST 기준이며, k6가 같은 날짜를 쓰도록 파일에 남긴다.
        LocalDate today = LocalDate.now(TimeConstants.KST);
        LocalDate yesterday = today.minusDays(1);
        List<Long> userIds = tokens.stream().map(t -> (Long) t.get("userId")).toList();
        int friendships = createFriendships(userIds);
        createDrinkHistory(userIds, List.of(today, yesterday));
        createFcmTokens(userIds);
        Long cheerTemplateId = activeCheerTemplateId();

        Map<String, Object> file = new LinkedHashMap<>();
        file.put("generatedAt", OffsetDateTime.now().withNano(0).toString());
        file.put("expiresInHours", TOKEN_TTL.toHours());
        file.put("cheerTemplateId", cheerTemplateId);
        file.put("today", today.toString());
        file.put("yesterday", yesterday.toString());
        file.put("tokens", tokens);

        Files.createDirectories(TOKEN_FILE.getParent());
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(TOKEN_FILE.toFile(), file);
        Files.setPosixFilePermissions(TOKEN_FILE, PosixFilePermissions.fromString("rw-------"));

        Set<Object> distinctIds = tokens.stream().map(t -> t.get("userId")).collect(Collectors.toSet());
        assertEquals(count, distinctIds.size(), "테스트 유저 ID가 중복되었습니다.");
        System.out.println("[loadtest] 테스트 유저 " + count + "명 준비 완료, 토큰 저장: " + TOKEN_FILE
                + " (" + TOKEN_TTL.toHours() + "시간 유효)");
        System.out.println("[loadtest] 친구 관계 " + friendships + "건(테스트 유저 간 전체), 음주 기록 " + today + "·" + yesterday
                + ", 가짜 FCM 토큰 유저당 " + FCM_TOKENS_PER_USER + "개, 응원 문구 ID " + cheerTemplateId);
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "LOADTEST_MODE", matches = "reset")
    void reset() {
        int complaints = deleteComplaints();
        int cheers = deleteCheers();
        System.out.println("[loadtest] 신고 데이터 " + complaints + "건, 응원 " + cheers + "건 삭제");
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "LOADTEST_MODE", matches = "cleanup")
    void cleanup() throws IOException {
        int complaints = deleteComplaints();
        int cheers = deleteCheers();
        // 테스트 유저가 한쪽이라도 걸린 행만 지운다. 실제 유저끼리의 행은 건드리지 않는다. 유저를 지우기 전에 자식 행부터 지운다.
        int friendRequests = jdbcTemplate.update(
                "DELETE FROM friend_request WHERE requester_id IN (" + LOADTEST_USER_IDS + ") OR receiver_id IN (" + LOADTEST_USER_IDS + ")",
                EMAIL_LIKE, EMAIL_LIKE);
        int blocks = jdbcTemplate.update(
                "DELETE FROM user_block WHERE blocker_id IN (" + LOADTEST_USER_IDS + ") OR blocked_id IN (" + LOADTEST_USER_IDS + ")",
                EMAIL_LIKE, EMAIL_LIKE);
        int friendships = jdbcTemplate.update(
                "DELETE FROM friendship WHERE user_low_id IN (" + LOADTEST_USER_IDS + ") OR user_high_id IN (" + LOADTEST_USER_IDS + ")",
                EMAIL_LIKE, EMAIL_LIKE);
        int fcmTokens = jdbcTemplate.update("DELETE FROM fcm_token WHERE user_id IN (" + LOADTEST_USER_IDS + ")", EMAIL_LIKE);
        int drinks = jdbcTemplate.update("DELETE FROM drink_history WHERE user_id IN (" + LOADTEST_USER_IDS + ")", EMAIL_LIKE);
        int users = jdbcTemplate.update("DELETE FROM `user` WHERE email LIKE ?", EMAIL_LIKE);
        Files.deleteIfExists(TOKEN_FILE);
        System.out.println("[loadtest] 신고 " + complaints + "건, 응원 " + cheers + "건, 친구 요청 " + friendRequests + "건, 차단 " + blocks
                + "건, 친구 관계 " + friendships + "건, FCM 토큰 " + fcmTokens + "건, 음주 기록 " + drinks + "건, 테스트 유저 " + users
                + "명, 토큰 파일 삭제");
    }

    private int deleteComplaints() {
        return jdbcTemplate.update(
                "DELETE FROM user_complaint WHERE reporter_id IN (" + LOADTEST_USER_IDS + ")"
                        + " OR target_user_id IN (" + LOADTEST_USER_IDS + ")",
                EMAIL_LIKE, EMAIL_LIKE);
    }

    private int deleteCheers() {
        return jdbcTemplate.update(
                "DELETE FROM cheer WHERE sender_id IN (" + LOADTEST_USER_IDS + ")"
                        + " OR receiver_id IN (" + LOADTEST_USER_IDS + ")",
                EMAIL_LIKE, EMAIL_LIKE);
    }

    /** 테스트 유저끼리 모두 친구로 만든다. Friendship.of로 정규화(low &lt; high)하고, 이미 있으면 건너뛴다. 대량이라 배치로 넣는다. */
    private int createFriendships(List<Long> userIds) {
        List<Object[]> rows = new ArrayList<>();
        for (int i = 0; i < userIds.size(); i++) {
            for (int j = i + 1; j < userIds.size(); j++) {
                Friendship friendship = Friendship.of(userIds.get(i), userIds.get(j));
                rows.add(new Object[]{friendship.getUserLowId(), friendship.getUserHighId()});
            }
        }
        jdbcTemplate.batchUpdate(
                "INSERT IGNORE INTO friendship (user_low_id, user_high_id, created_at, updated_at) VALUES (?, ?, NOW(), NOW())",
                rows, 1000, (ps, row) -> {
                    ps.setLong(1, (Long) row[0]);
                    ps.setLong(2, (Long) row[1]);
                });
        return rows.size();
    }

    /** 응원은 "그날 상대가 실제로 마셨을 때"만 보낼 수 있으므로, 테스트 유저 모두에게 해당 날짜의 음주 기록(is_drink=1)을 만든다. */
    private void createDrinkHistory(List<Long> userIds, List<LocalDate> dates) {
        List<Object[]> rows = new ArrayList<>();
        for (Long userId : userIds) {
            for (LocalDate date : dates) {
                rows.add(new Object[]{userId, java.sql.Date.valueOf(date)});
            }
        }
        jdbcTemplate.batchUpdate(
                "INSERT INTO drink_history (user_id, drink_date, is_drink, created_at, updated_at) "
                        + "SELECT ?, ?, 1, NOW(), NOW() FROM DUAL "
                        + "WHERE NOT EXISTS (SELECT 1 FROM drink_history WHERE user_id = ? AND drink_date = ?)",
                rows, 1000, (ps, row) -> {
                    ps.setLong(1, (Long) row[0]);
                    ps.setDate(2, (java.sql.Date) row[1]);
                    ps.setLong(3, (Long) row[0]);
                    ps.setDate(4, (java.sql.Date) row[1]);
                });
        // 이전 실행이나 다른 이유로 같은 날짜에 is_drink=0 행이 있었다면 마신 것으로 바로잡는다. (테스트 유저만)
        for (LocalDate date : dates) {
            jdbcTemplate.update("UPDATE drink_history SET is_drink = 1 WHERE drink_date = ? AND user_id IN (" + LOADTEST_USER_IDS + ")",
                    java.sql.Date.valueOf(date), EMAIL_LIKE);
        }
    }

    /** 푸시 경로가 끝까지 가도록 유저마다 가짜 FCM 토큰을 만든다. 형식만 토큰 모양인 임의 문자열이며 실제 기기와 무관하다. */
    private void createFcmTokens(List<Long> userIds) {
        List<Object[]> rows = new ArrayList<>();
        for (Long userId : userIds) {
            for (int device = 1; device <= FCM_TOKENS_PER_USER; device++) {
                rows.add(new Object[]{userId, "loadtest-fake-fcm-token-" + userId + "-" + device});
            }
        }
        jdbcTemplate.batchUpdate(
                "INSERT IGNORE INTO fcm_token (user_id, fcm_token, created_at, updated_at) VALUES (?, ?, NOW(), NOW())",
                rows, 1000, (ps, row) -> {
                    ps.setLong(1, (Long) row[0]);
                    ps.setString(2, (String) row[1]);
                });
    }

    /** 응원 문구는 서버가 처음 뜰 때 테이블이 비어 있으면 시드한다. 활성 문구가 없으면 시드가 안 된 것이므로 중단한다. */
    private Long activeCheerTemplateId() {
        List<Long> ids = jdbcTemplate.queryForList(
                "SELECT cheer_template_id FROM cheer_template WHERE is_active = 1 ORDER BY cheer_template_id LIMIT 1", Long.class);
        assertTrue(!ids.isEmpty(), "활성 응원 문구가 없습니다. 앱을 한 번 기동해 CheerTemplateInitializer가 시드하게 하세요.");
        return ids.get(0);
    }

    private static int userCount() {
        String value = System.getenv("LOADTEST_USERS");
        int count = value == null || value.isBlank() ? 200 : Integer.parseInt(value.trim());
        assertTrue(count >= 2 && count <= 2000, "LOADTEST_USERS는 2~2000 사이여야 합니다: " + count);
        return count;
    }

    // jdbc:mysql://host:port/db?... 에서 호스트만 꺼내 검사한다.
    static boolean isLocalHost(String jdbcUrl) {
        if (jdbcUrl == null) {
            return false;
        }
        int start = jdbcUrl.indexOf("//");
        if (start < 0) {
            return false;
        }
        String rest = jdbcUrl.substring(start + 2);
        int end = rest.length();
        for (char c : new char[]{':', '/', '?'}) {
            int idx = rest.indexOf(c);
            if (idx >= 0) {
                end = Math.min(end, idx);
            }
        }
        String host = rest.substring(0, end);
        return host.equals("localhost") || host.equals("127.0.0.1");
    }
}
