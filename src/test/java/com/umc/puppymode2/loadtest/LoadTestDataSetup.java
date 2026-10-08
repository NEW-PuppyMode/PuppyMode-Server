package com.umc.puppymode2.loadtest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.puppymode2.domain.user.auth.enums.Provider;
import com.umc.puppymode2.domain.user.entity.User;
import com.umc.puppymode2.domain.user.entity.enums.UserStatus;
import com.umc.puppymode2.domain.user.repository.UserRepository;
import com.umc.puppymode2.global.auth.token.JwtTokenProvider;
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
 * 로컬 부하 테스트(#214)용 테스트 유저와 JWT를 만들고 정리하는 도구.
 *
 * 평소 {@code ./gradlew test}에서는 환경 변수 LOADTEST_MODE가 없어 모두 건너뛴다.
 * 직접 실행할 때는 scripts/perf/loadtest-data.sh 를 사용한다.
 *
 *   setup   : 테스트 유저를 멱등하게 만들고 scripts/perf/out/tokens.json 에 토큰을 쓴다
 *   reset   : 테스트 유저가 관련된 신고 데이터만 지운다 (UNIQUE 충돌 없이 시나리오를 다시 돌리기 위함)
 *   cleanup : reset + 테스트 유저와 토큰 파일 삭제
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

        Map<String, Object> file = new LinkedHashMap<>();
        file.put("generatedAt", OffsetDateTime.now().withNano(0).toString());
        file.put("expiresInHours", TOKEN_TTL.toHours());
        file.put("tokens", tokens);

        Files.createDirectories(TOKEN_FILE.getParent());
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(TOKEN_FILE.toFile(), file);
        Files.setPosixFilePermissions(TOKEN_FILE, PosixFilePermissions.fromString("rw-------"));

        Set<Object> distinctIds = tokens.stream().map(t -> t.get("userId")).collect(Collectors.toSet());
        assertEquals(count, distinctIds.size(), "테스트 유저 ID가 중복되었습니다.");
        System.out.println("[loadtest] 테스트 유저 " + count + "명 준비 완료, 토큰 저장: " + TOKEN_FILE
                + " (" + TOKEN_TTL.toHours() + "시간 유효)");
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "LOADTEST_MODE", matches = "reset")
    void reset() {
        int deleted = deleteComplaints();
        System.out.println("[loadtest] 신고 데이터 " + deleted + "건 삭제");
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "LOADTEST_MODE", matches = "cleanup")
    void cleanup() throws IOException {
        int complaints = deleteComplaints();
        int users = jdbcTemplate.update("DELETE FROM `user` WHERE email LIKE ?", EMAIL_LIKE);
        Files.deleteIfExists(TOKEN_FILE);
        System.out.println("[loadtest] 신고 " + complaints + "건, 테스트 유저 " + users + "명, 토큰 파일 삭제");
    }

    private int deleteComplaints() {
        return jdbcTemplate.update(
                "DELETE FROM user_complaint WHERE reporter_id IN (" + LOADTEST_USER_IDS + ")"
                        + " OR target_user_id IN (" + LOADTEST_USER_IDS + ")",
                EMAIL_LIKE, EMAIL_LIKE);
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
