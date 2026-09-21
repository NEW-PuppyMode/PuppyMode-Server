package com.umc.puppymode2.domain.friend.service;

import com.umc.puppymode2.domain.friend.dto.FriendCheerState;
import com.umc.puppymode2.domain.friend.dto.FriendDrinkStatus;
import com.umc.puppymode2.domain.friend.service.FriendStatusCalculator.FriendStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FriendStatusCalculatorTest {

    private final FriendStatusCalculator calculator = new FriendStatusCalculator();

    private final LocalDate today = LocalDate.of(2026, 9, 21);
    private final LocalDate yesterday = today.minusDays(1);

    @Test
    void 오늘_음주하면_DRANK_TODAY이고_오늘_응원이_ACTIVE() {
        FriendStatus status = calculator.calculate(today, Map.of(today, true), Set.of());

        assertEquals(FriendDrinkStatus.DRANK_TODAY, status.drinkStatus());
        assertEquals(FriendCheerState.ACTIVE, status.cheerState());
        assertEquals(today, status.cheerTargetDate());
    }

    @Test
    void 어제만_음주하면_DRANK_YESTERDAY이고_어제_응원이_ACTIVE() {
        FriendStatus status = calculator.calculate(today, Map.of(yesterday, true), Set.of());

        assertEquals(FriendDrinkStatus.DRANK_YESTERDAY, status.drinkStatus());
        assertEquals(FriendCheerState.ACTIVE, status.cheerState());
        assertEquals(yesterday, status.cheerTargetDate());
    }

    @Test
    void 이틀_연속_음주면_어제가_남아있는_동안_어제를_우선한다() {
        FriendStatus status = calculator.calculate(today, Map.of(yesterday, true, today, true), Set.of());

        assertEquals(FriendDrinkStatus.DRANK_TODAY, status.drinkStatus());
        assertEquals(FriendCheerState.ACTIVE, status.cheerState());
        assertEquals(yesterday, status.cheerTargetDate());
    }

    @Test
    void 이틀_연속_음주에서_어제_응원을_보내면_오늘로_넘어가며_ACTIVE가_유지된다() {
        FriendStatus status = calculator.calculate(
                today, Map.of(yesterday, true, today, true), Set.of(yesterday));

        assertEquals(FriendCheerState.ACTIVE, status.cheerState());
        assertEquals(today, status.cheerTargetDate());
    }

    @Test
    void 이틀_연속_음주에서_둘_다_응원했으면_SENT() {
        FriendStatus status = calculator.calculate(
                today, Map.of(yesterday, true, today, true), Set.of(yesterday, today));

        assertEquals(FriendCheerState.SENT, status.cheerState());
        assertNull(status.cheerTargetDate());
    }

    @Test
    void 어제_음주했고_어제_응원을_보냈으면_SENT() {
        FriendStatus status = calculator.calculate(today, Map.of(yesterday, true), Set.of(yesterday));

        assertEquals(FriendDrinkStatus.DRANK_YESTERDAY, status.drinkStatus());
        assertEquals(FriendCheerState.SENT, status.cheerState());
        assertNull(status.cheerTargetDate());
    }

    @Test
    void 오늘_기록이_false이면_NOT_DRANK이고_응원은_DISABLED() {
        FriendStatus status = calculator.calculate(today, Map.of(today, false), Set.of());

        assertEquals(FriendDrinkStatus.NOT_DRANK, status.drinkStatus());
        assertEquals(FriendCheerState.DISABLED, status.cheerState());
        assertNull(status.cheerTargetDate());
    }

    @Test
    void 기록이_전혀_없으면_NONE이고_응원은_DISABLED() {
        FriendStatus status = calculator.calculate(today, Map.of(), Set.of());

        assertEquals(FriendDrinkStatus.NONE, status.drinkStatus());
        assertEquals(FriendCheerState.DISABLED, status.cheerState());
    }

    @Test
    void 오늘_false_어제_true이면_어제_문구가_우선한다() {
        FriendStatus status = calculator.calculate(today, Map.of(today, false, yesterday, true), Set.of());

        assertEquals(FriendDrinkStatus.DRANK_YESTERDAY, status.drinkStatus());
        assertEquals(FriendCheerState.ACTIVE, status.cheerState());
        assertEquals(yesterday, status.cheerTargetDate());
    }

    @Test
    void 자정이_지나_날짜가_바뀌면_어제_음주가_그제로_밀려나_응원이_사라진다() {
        // 9/21 음주 기록 하나만 있는 친구를 기준으로 날짜만 바꿔가며 본다.
        //  - 9/21 : 오늘 음주 → 오늘 건 응원 가능
        //  - 9/22 : 자정이 지나 그 기록이 "어제"가 됨 → 어제 건으로 응원 가능 (음주 문구도 DRANK_YESTERDAY)
        //  - 9/23 : 이틀이 지나 어제/오늘 어디에도 해당 없음 → 응원 불가
        Map<LocalDate, Boolean> records = Map.of(today, true);

        FriendStatus before = calculator.calculate(today, records, Set.of());
        FriendStatus afterMidnight = calculator.calculate(today.plusDays(1), records, Set.of());
        FriendStatus twoDaysLater = calculator.calculate(today.plusDays(2), records, Set.of());

        assertEquals(today, before.cheerTargetDate());
        assertEquals(FriendDrinkStatus.DRANK_YESTERDAY, afterMidnight.drinkStatus());
        assertEquals(today, afterMidnight.cheerTargetDate());
        // 이틀이 지나면 더는 응원할 수 없다.
        assertEquals(FriendDrinkStatus.NONE, twoDaysLater.drinkStatus());
        assertEquals(FriendCheerState.DISABLED, twoDaysLater.cheerState());
    }
}
