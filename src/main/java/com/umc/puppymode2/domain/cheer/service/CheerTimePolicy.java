package com.umc.puppymode2.domain.cheer.service;

import com.umc.puppymode2.domain.cheer.dto.CheerExpiresIn;
import com.umc.puppymode2.global.util.TimeConstants;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * 응원의 날짜·만료 규칙 (설계서 5-6, 5-7).
 *
 * 현재 시각을 직접 읽지 않고 호출하는 쪽이 넘겨주는 순수 계산이라, 자정 경계(23:59 / 00:00)를
 * 시각만 바꿔가며 단위 테스트할 수 있다.
 */
@Component
public class CheerTimePolicy {

    /** 응원할 수 있는 날짜인가: 응원 대상 음주 날짜는 어제 또는 오늘(KST)이어야 한다. */
    public boolean isCheerableDate(LocalDate todayKst, LocalDate targetDate) {
        return targetDate.equals(todayKst) || targetDate.equals(todayKst.minusDays(1));
    }

    /**
     * 만료 시각 = (오늘 + 2일) 00:00 KST, 즉 "받은 다음 날 밤 12시".
     *
     * 자정 경계 예: KST 9/21 23:59에 보내면 9/23 00:00, 9/22 00:00에 보내면 9/24 00:00에 만료된다.
     * 저장은 다른 시각 컬럼(created_at)과 같은 기준인 JVM 기본 타임존의 LocalDateTime으로 한다.
     * (KST 자정이라는 "순간"은 그대로이고, 표현만 서버 타임존으로 바뀐다)
     */
    public LocalDateTime calculateExpiresAt(ZonedDateTime nowKst) {
        return nowKst.toLocalDate()
                .plusDays(2)
                .atStartOfDay(TimeConstants.KST)
                .withZoneSameInstant(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    /**
     * 이 응원이 오늘 밤에 사라지는지(TODAY), 내일 밤에 사라지는지(TOMORROW).
     *
     * 만료 시각은 항상 자정(00:00)이므로 "사라지는 밤"은 만료 시각 직전의 날짜다.
     * 예: 9/23 00:00 만료 = 9/22 밤 12시에 사라짐. 9/21에 보면 TOMORROW, 9/22에 보면 TODAY.
     */
    public CheerExpiresIn calculateExpiresIn(LocalDateTime expiresAt, LocalDate todayKst) {
        LocalDate vanishNight = expiresAt.atZone(ZoneId.systemDefault())
                .withZoneSameInstant(TimeConstants.KST)
                .minusNanos(1)
                .toLocalDate();
        return vanishNight.isAfter(todayKst) ? CheerExpiresIn.TOMORROW : CheerExpiresIn.TODAY;
    }
}
