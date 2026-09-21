package com.umc.puppymode2.domain.friend.entity;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;

class FriendRequestTest {

    private TimeZone originalTimeZone;

    @BeforeEach
    void saveTimeZone() {
        originalTimeZone = TimeZone.getDefault();
    }

    @AfterEach
    void restoreTimeZone() {
        TimeZone.setDefault(originalTimeZone);
    }

    // respondedAt은 같은 행의 created_at(JPA Auditing = LocalDateTime.now(), JVM 기본 타임존)과 같은 기준이어야 한다.
    // 예전처럼 KST로 저장하면 JVM 타임존이 UTC일 때 9시간 어긋나 이 테스트가 실패한다.
    @Test
    void 서버_타임존이_UTC여도_수락_시각은_JVM_기본_타임존_기준으로_기록된다() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        FriendRequest request = FriendRequest.create(1L, 2L);

        LocalDateTime before = LocalDateTime.now();
        request.accept();
        LocalDateTime after = LocalDateTime.now();

        assertFalse(request.getRespondedAt().isBefore(before));
        assertFalse(request.getRespondedAt().isAfter(after));
    }

    @Test
    void 서버_타임존이_UTC여도_거절_시각은_JVM_기본_타임존_기준으로_기록된다() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        FriendRequest request = FriendRequest.create(1L, 2L);

        LocalDateTime before = LocalDateTime.now();
        request.reject();
        LocalDateTime after = LocalDateTime.now();

        assertFalse(request.getRespondedAt().isBefore(before));
        assertFalse(request.getRespondedAt().isAfter(after));
    }

    @Test
    void 서버_타임존이_KST일_때도_같은_기준이다() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
        FriendRequest request = FriendRequest.create(1L, 2L);

        request.accept();

        assertTrue(Duration.between(request.getRespondedAt(), LocalDateTime.now()).abs().getSeconds() < 5);
    }

    @Test
    void 재요청으로_되돌리면_응답_시각이_비워진다() {
        FriendRequest request = FriendRequest.create(1L, 2L);
        request.reject();

        request.reopen();

        assertTrue(request.isPending());
        assertNull(request.getRespondedAt());
    }
}
