package com.umc.puppymode2.domain.friend.converter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;

class FriendConverterTest {

    private final FriendConverter converter = new FriendConverter();

    private TimeZone originalTimeZone;

    @BeforeEach
    void saveTimeZone() {
        originalTimeZone = TimeZone.getDefault();
    }

    @AfterEach
    void restoreTimeZone() {
        TimeZone.setDefault(originalTimeZone);
    }

    @Test
    void 서버_타임존이_UTC이면_9시간을_더해_KST로_변환한다() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        OffsetDateTime result = converter.toKstOffset(LocalDateTime.of(2026, 9, 21, 1, 12, 0));

        assertEquals("2026-09-21T10:12+09:00", result.toString());
    }

    @Test
    void 서버_타임존이_KST이면_시각은_그대로이고_오프셋만_붙는다() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));

        OffsetDateTime result = converter.toKstOffset(LocalDateTime.of(2026, 9, 21, 10, 12, 0));

        assertEquals("2026-09-21T10:12+09:00", result.toString());
    }

    @Test
    void 서버_타임존과_무관하게_같은_시각은_같은_순간을_가리킨다() {
        // created_at(Auditing)과 responded_at이 같은 기준으로 저장되므로, 서버 타임존이 달라도
        // 같은 순간을 나타내는 두 값은 변환 후 동일해야 한다.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        OffsetDateTime fromUtcServer = converter.toKstOffset(LocalDateTime.of(2026, 9, 21, 1, 0, 0));

        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
        OffsetDateTime fromKstServer = converter.toKstOffset(LocalDateTime.of(2026, 9, 21, 10, 0, 0));

        assertEquals(fromUtcServer.toInstant(), fromKstServer.toInstant());
    }

    @Test
    void null은_null로_반환한다() {
        assertNull(converter.toKstOffset(null));
    }
}
