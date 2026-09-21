package com.umc.puppymode2.domain.friend.service;

import com.umc.puppymode2.domain.friend.dto.FriendCheerState;
import com.umc.puppymode2.domain.friend.dto.FriendDrinkStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

/**
 * 친구 한 명의 「음주 문구 상태」와 「응원하기 버튼 상태」를 계산한다. (설계서 5-1)
 *
 * DB나 현재 시각에 의존하지 않는 순수 계산이라, 자정 경계·이틀 연속 음주 같은 규칙을
 * today 값만 바꿔가며 단위 테스트할 수 있다.
 */
@Component
public class FriendStatusCalculator {

    /**
     * @param today       KST 기준 오늘
     * @param drinkByDate 친구의 날짜별 음주 여부. 기록이 있는 날짜만 키로 존재하며,
     *                    값은 그 날짜에 is_drink = true 인 행이 하나라도 있는지다.
     *                    (하루에 행이 여러 개여도 하나라도 true면 마신 것으로 본다 — DrinkHistory 유니크 미확인 대비)
     * @param sentDates   내가 이 친구에게 이미 응원을 보낸 대상 날짜들
     */
    public FriendStatus calculate(LocalDate today, Map<LocalDate, Boolean> drinkByDate, Set<LocalDate> sentDates) {
        LocalDate yesterday = today.minusDays(1);

        boolean drankToday = Boolean.TRUE.equals(drinkByDate.get(today));
        boolean drankYesterday = Boolean.TRUE.equals(drinkByDate.get(yesterday));
        boolean sentToday = sentDates.contains(today);
        boolean sentYesterday = sentDates.contains(yesterday);

        // 응원 대상 날짜: 어제 건이 남아 있으면 어제를 먼저, 없으면 오늘.
        // (이틀 연속 음주라면 어제 응원을 보낸 뒤 다시 조회했을 때 오늘로 넘어가며 ACTIVE가 유지된다)
        LocalDate targetDate = null;
        if (drankYesterday && !sentYesterday) {
            targetDate = yesterday;
        } else if (drankToday && !sentToday) {
            targetDate = today;
        }

        FriendCheerState cheerState;
        if (targetDate != null) {
            cheerState = FriendCheerState.ACTIVE;
        } else if (drankYesterday || drankToday) {
            cheerState = FriendCheerState.SENT;
        } else {
            cheerState = FriendCheerState.DISABLED;
        }

        FriendDrinkStatus drinkStatus;
        if (drankToday) {
            drinkStatus = FriendDrinkStatus.DRANK_TODAY;
        } else if (drankYesterday) {
            drinkStatus = FriendDrinkStatus.DRANK_YESTERDAY;
        } else if (drinkByDate.containsKey(today)) {
            // 오늘 기록 행은 있는데 is_drink = false
            drinkStatus = FriendDrinkStatus.NOT_DRANK;
        } else {
            drinkStatus = FriendDrinkStatus.NONE;
        }

        return new FriendStatus(drinkStatus, cheerState, targetDate);
    }

    public record FriendStatus(FriendDrinkStatus drinkStatus, FriendCheerState cheerState, LocalDate cheerTargetDate) {
    }
}
