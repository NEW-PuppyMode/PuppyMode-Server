package com.umc.puppymode2.domain.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 로컬 부하 테스트용 가짜 FCM. 실제 FCM을 호출하지 않는다.
 *
 * <p><b>명시적으로 켰을 때만 활성화된다</b>: {@code loadtest.fake-fcm.enabled=true}.
 * 켜져 있는데 활성 DB가 localhost가 아니면 기동에 실패한다. (운영/개발 환경에서 실수로 켜져 알림이 조용히 사라지는 것을 막는다)
 *
 * <p>응답 지연과 실패는 프로퍼티로 정하고, 앱을 재시작하지 않고 바꾸려면 제어 파일(JSON)을 쓴다.
 * 제어 파일은 {@code loadtest.fake-fcm.control-file}에 경로를 주면 변경 시각이 바뀔 때마다 다시 읽는다.
 * <pre>{"delayMs": 300, "slowPct": 5, "slowDelayMs": 4000, "failurePct": 0, "failureCode": "UNAVAILABLE"}</pre>
 * slowPct가 0보다 크면 호출 순번 기준으로 100건 중 slowPct건만 slowDelayMs, 나머지는 delayMs만큼 걸린다.
 * failurePct는 토큰 순번 기준으로 그 비율만큼 failureCode로 실패시킨다. (UNREGISTERED·INVALID_ARGUMENT는 토큰 삭제를 일으키므로 쓰지 않는다)
 *
 * <p>집계는 엔드포인트 없이 메트릭으로만 노출한다: fake_fcm_calls_total, fake_fcm_tokens_total,
 * fake_fcm_failed_tokens_total, fake_fcm_inflight (피크는 부하 도구가 샘플링한다).
 * 토큰 값은 어디에도 기록하지 않는다.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "loadtest.fake-fcm.enabled", havingValue = "true")
public class FakeFcmClient implements FcmClient {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final File controlFile;
    private volatile Settings settings;
    private volatile long controlFileModified = -1;

    private final AtomicLong callSeq = new AtomicLong();
    private final AtomicLong tokenSeq = new AtomicLong();
    private final AtomicInteger inflight = new AtomicInteger();
    private final Counter calls;
    private final Counter tokens;
    private final Counter failedTokens;

    public FakeFcmClient(MeterRegistry registry,
                         @Value("${spring.datasource.url}") String datasourceUrl,
                         @Value("${loadtest.fake-fcm.delay-ms:0}") long delayMs,
                         @Value("${loadtest.fake-fcm.slow-pct:0}") int slowPct,
                         @Value("${loadtest.fake-fcm.slow-delay-ms:0}") long slowDelayMs,
                         @Value("${loadtest.fake-fcm.failure-pct:0}") int failurePct,
                         @Value("${loadtest.fake-fcm.failure-code:UNAVAILABLE}") String failureCode,
                         @Value("${loadtest.fake-fcm.control-file:}") String controlFilePath) {
        requireLocalDatabase(datasourceUrl);
        this.settings = new Settings(delayMs, slowPct, slowDelayMs, failurePct, parseCode(failureCode));
        this.controlFile = controlFilePath == null || controlFilePath.isBlank() ? null : new File(controlFilePath);
        this.calls = Counter.builder("fake_fcm_calls").description("가짜 FCM sendEach 호출 수").register(registry);
        this.tokens = Counter.builder("fake_fcm_tokens").description("가짜 FCM이 받은 메시지(토큰) 수").register(registry);
        this.failedTokens = Counter.builder("fake_fcm_failed_tokens").description("가짜 FCM이 실패시킨 메시지 수").register(registry);
        Gauge.builder("fake_fcm_inflight", inflight, AtomicInteger::get).description("진행 중인 가짜 FCM 호출 수").register(registry);
        log.warn("[FAKE FCM] 가짜 FCM이 활성화되었습니다. 실제 푸시는 발송되지 않습니다. (loadtest.fake-fcm.enabled=true)");
    }

    @Override
    public BatchResult sendEach(List<Message> messages) throws FirebaseMessagingException {
        Settings current = currentSettings();
        long call = callSeq.getAndIncrement();
        calls.increment();
        tokens.increment(messages.size());
        inflight.incrementAndGet();
        try {
            sleep(current.delayFor(call));

            List<Item> items = new ArrayList<>(messages.size());
            int failures = 0;
            for (int i = 0; i < messages.size(); i++) {
                long seq = tokenSeq.getAndIncrement();
                if (current.failurePct() > 0 && (seq % 100) < current.failurePct()) {
                    items.add(new Item(false, null, current.failureCode(), "fake failure"));
                    failures++;
                } else {
                    items.add(new Item(true, "fake-message-id", null, null));
                }
            }
            failedTokens.increment(failures);
            return new BatchResult(messages.size() - failures, failures, items);
        } finally {
            inflight.decrementAndGet();
        }
    }

    private Settings currentSettings() {
        if (controlFile != null) {
            long modified = controlFile.lastModified();
            if (modified != controlFileModified) {
                synchronized (this) {
                    if (modified != controlFileModified) {
                        reloadControlFile(modified);
                    }
                }
            }
        }
        return settings;
    }

    private void reloadControlFile(long modified) {
        try {
            ControlFile loaded = objectMapper.readValue(controlFile, ControlFile.class);
            Settings base = settings;
            settings = new Settings(
                    loaded.delayMs != null ? loaded.delayMs : base.delayMs(),
                    loaded.slowPct != null ? loaded.slowPct : base.slowPct(),
                    loaded.slowDelayMs != null ? loaded.slowDelayMs : base.slowDelayMs(),
                    loaded.failurePct != null ? loaded.failurePct : base.failurePct(),
                    loaded.failureCode != null ? parseCode(loaded.failureCode) : base.failureCode());
            controlFileModified = modified;
            log.warn("[FAKE FCM] 제어 파일을 다시 읽었습니다: {}", settings);
        } catch (Exception e) {
            controlFileModified = modified; // 같은 파일을 반복해서 읽다 실패하지 않도록 변경 시각은 갱신한다
            log.warn("[FAKE FCM] 제어 파일을 읽지 못해 이전 설정을 유지합니다: {}", e.getClass().getSimpleName());
        }
    }

    private static void sleep(long millis) {
        if (millis <= 0) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static MessagingErrorCode parseCode(String code) {
        try {
            return MessagingErrorCode.valueOf(code.trim().toUpperCase());
        } catch (RuntimeException e) {
            throw new IllegalStateException("loadtest.fake-fcm.failure-code가 올바르지 않습니다.");
        }
    }

    /** 가짜 FCM은 localhost DB에서만 켤 수 있다. jdbc:mysql://host:port/db?... 에서 호스트만 꺼내 검사한다. */
    static void requireLocalDatabase(String jdbcUrl) {
        if (!isLocalHost(jdbcUrl)) {
            throw new IllegalStateException(
                    "loadtest.fake-fcm.enabled=true 는 localhost DB에서만 사용할 수 있습니다. (활성 DB 호스트가 localhost가 아님)");
        }
    }

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

    private record Settings(long delayMs, int slowPct, long slowDelayMs, int failurePct, MessagingErrorCode failureCode) {
        long delayFor(long callSeq) {
            return slowPct > 0 && (callSeq % 100) < slowPct ? slowDelayMs : delayMs;
        }
    }

    /** 제어 파일 형식. 빠진 항목은 이전 설정을 유지한다. */
    public static class ControlFile {
        public Long delayMs;
        public Integer slowPct;
        public Long slowDelayMs;
        public Integer failurePct;
        public String failureCode;
    }
}
