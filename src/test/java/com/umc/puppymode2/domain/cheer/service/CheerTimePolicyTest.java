package com.umc.puppymode2.domain.cheer.service;

import com.umc.puppymode2.domain.cheer.dto.CheerExpiresIn;
import com.umc.puppymode2.global.util.TimeConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;

class CheerTimePolicyTest {

    private final CheerTimePolicy policy = new CheerTimePolicy();

    private TimeZone originalTimeZone;

    @BeforeEach
    void saveTimeZone() {
        originalTimeZone = TimeZone.getDefault();
    }

    @AfterEach
    void restoreTimeZone() {
        TimeZone.setDefault(originalTimeZone);
    }

    private final LocalDate today = LocalDate.of(2026, 9, 21);

    // ---------------------------------------------------------------- 응원 가능 날짜

    @Test
    void 오늘과_어제만_응원할_수_있다() {
        assertTrue(policy.isCheerableDate(today, today));
        assertTrue(policy.isCheerableDate(today, today.minusDays(1)));
    }

    @Test
    void 그제_이전과_미래_날짜는_응원할_수_없다() {
        assertFalse(policy.isCheerableDate(today, today.minusDays(2)));
        assertFalse(policy.isCheerableDate(today, today.plusDays(1)));
    }

    @Test
    void 자정이_지나_날짜가_바뀌면_어제_건이_그제가_되어_응원할_수_없다() {
        // 9/21 23:59에는 9/20(어제)을 응원할 수 있지만, 9/22 00:00이 되면 9/20은 그제가 된다.
        LocalDate target = LocalDate.of(2026, 9, 20);

        assertTrue(policy.isCheerableDate(LocalDate.of(2026, 9, 21), target));
        assertFalse(policy.isCheerableDate(LocalDate.of(2026, 9, 22), target));
    }

    // ---------------------------------------------------------------- 만료 시각

    @Test
    void 만료_시각은_오늘_더하기_2일_00시_KST이다() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));

        LocalDateTime expiresAt = policy.calculateExpiresAt(kst(2026, 9, 21, 21, 30));

        assertEquals(LocalDateTime.of(2026, 9, 23, 0, 0), expiresAt);
    }

    @Test
    void 자정_직전_23시59분에_보내면_모레_00시에_만료된다() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));

        LocalDateTime expiresAt = policy.calculateExpiresAt(kst(2026, 9, 21, 23, 59));

        assertEquals(LocalDateTime.of(2026, 9, 23, 0, 0), expiresAt);
    }

    @Test
    void 자정_정각_00시00분에_보내면_날짜가_바뀌어_하루_늦게_만료된다() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));

        LocalDateTime expiresAt = policy.calculateExpiresAt(kst(2026, 9, 22, 0, 0));

        assertEquals(LocalDateTime.of(2026, 9, 24, 0, 0), expiresAt);
    }

    @Test
    void 서버_타임존이_UTC여도_KST_자정이라는_같은_순간에_만료된다() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        LocalDateTime expiresAt = policy.calculateExpiresAt(kst(2026, 9, 21, 21, 30));

        // 저장 값은 UTC 표현(9/22 15:00)이지만 가리키는 순간은 KST 9/23 00:00이다.
        assertEquals(LocalDateTime.of(2026, 9, 22, 15, 0), expiresAt);
        assertEquals(
                ZonedDateTime.of(2026, 9, 23, 0, 0, 0, 0, TimeConstants.KST).toInstant(),
                expiresAt.atZone(ZoneId.systemDefault()).toInstant());
    }

    // ---------------------------------------------------------------- expiresIn

    @Test
    void 받은_당일에는_TOMORROW이다() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
        // 9/21에 받아 9/23 00:00 만료 = 9/22 밤 12시에 사라짐
        LocalDateTime expiresAt = LocalDateTime.of(2026, 9, 23, 0, 0);

        assertEquals(CheerExpiresIn.TOMORROW, policy.calculateExpiresIn(expiresAt, LocalDate.of(2026, 9, 21)));
    }

    @Test
    void 받은_다음날에는_TODAY이다() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
        LocalDateTime expiresAt = LocalDateTime.of(2026, 9, 23, 0, 0);

        assertEquals(CheerExpiresIn.TODAY, policy.calculateExpiresIn(expiresAt, LocalDate.of(2026, 9, 22)));
    }

    @Test
    void 자정_경계에서_23시59분까지는_TODAY이고_00시가_되면_만료되어_목록에서_빠진다() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
        LocalDateTime expiresAt = LocalDateTime.of(2026, 9, 23, 0, 0);

        // 9/22 23:59 : 아직 살아 있고 오늘 밤에 사라짐
        assertEquals(CheerExpiresIn.TODAY, policy.calculateExpiresIn(expiresAt, LocalDate.of(2026, 9, 22)));
        assertTrue(LocalDateTime.of(2026, 9, 22, 23, 59).isBefore(expiresAt));

        // 9/23 00:00 : 만료 시각 도달. 목록 조회는 expires_at > now 이므로 더는 나오지 않는다.
        assertFalse(expiresAt.isAfter(LocalDateTime.of(2026, 9, 23, 0, 0)));
    }

    @Test
    void 서버_타임존이_UTC여도_expiresIn은_KST_날짜로_판단한다() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        // KST 9/23 00:00 만료 = UTC 9/22 15:00 로 저장됨
        LocalDateTime expiresAt = LocalDateTime.of(2026, 9, 22, 15, 0);

        assertEquals(CheerExpiresIn.TOMORROW, policy.calculateExpiresIn(expiresAt, LocalDate.of(2026, 9, 21)));
        assertEquals(CheerExpiresIn.TODAY, policy.calculateExpiresIn(expiresAt, LocalDate.of(2026, 9, 22)));
    }

    private ZonedDateTime kst(int year, int month, int day, int hour, int minute) {
        return ZonedDateTime.of(year, month, day, hour, minute, 0, 0, TimeConstants.KST);
    }
}
