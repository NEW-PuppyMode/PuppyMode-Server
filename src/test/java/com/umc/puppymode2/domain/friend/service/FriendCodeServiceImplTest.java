package com.umc.puppymode2.domain.friend.service;

import com.umc.puppymode2.domain.friend.converter.FriendConverter;
import com.umc.puppymode2.domain.friend.dto.FriendCodeResponseDTO;
import com.umc.puppymode2.domain.friend.entity.FriendCode;
import com.umc.puppymode2.domain.friend.repository.FriendCodeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FriendCodeServiceImplTest {

    @Mock
    private FriendCodeRepository friendCodeRepository;

    @Spy
    private FriendConverter converter = new FriendConverter();

    @InjectMocks
    private FriendCodeServiceImpl service;

    private final Long userId = 1L;

    @Test
    void 이미_코드가_있으면_새로_발급하지_않고_같은_코드를_반환한다() {
        when(friendCodeRepository.findByUserId(userId)).thenReturn(Optional.of(FriendCode.of(userId, "4829")));

        FriendCodeResponseDTO result = service.getOrIssueMyCode(userId);

        assertEquals("4829", result.getCode());
        verify(friendCodeRepository, never()).saveAndFlush(any());
    }

    @Test
    void 코드가_없으면_4자리_숫자_코드를_발급한다() {
        when(friendCodeRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(friendCodeRepository.saveAndFlush(any(FriendCode.class))).thenAnswer(inv -> inv.getArgument(0));

        FriendCodeResponseDTO result = service.getOrIssueMyCode(userId);

        assertTrue(result.getCode().matches("^[1-9]\\d{3}$"), "맨 앞이 0이 아닌 4자리 숫자여야 합니다: " + result.getCode());
    }

    @Test
    void 다른_사용자와_코드가_충돌하면_새_코드로_재시도한다() {
        when(friendCodeRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(friendCodeRepository.saveAndFlush(any(FriendCode.class)))
                .thenThrow(new DataIntegrityViolationException("uk_friend_code_code"))
                .thenAnswer(inv -> inv.getArgument(0));

        FriendCodeResponseDTO result = service.getOrIssueMyCode(userId);

        assertNotNull(result.getCode());
        verify(friendCodeRepository, times(2)).saveAndFlush(any(FriendCode.class));
    }

    @Test
    void 동시에_최초_조회해서_다른_요청이_먼저_코드를_만들었다면_그_코드를_반환한다() {
        when(friendCodeRepository.findByUserId(userId))
                .thenReturn(Optional.empty())                                     // 최초 조회: 없음
                .thenReturn(Optional.of(FriendCode.of(userId, "7351")));          // 충돌 후 재조회: 다른 요청이 만든 코드
        when(friendCodeRepository.saveAndFlush(any(FriendCode.class)))
                .thenThrow(new DataIntegrityViolationException("uk_friend_code_user"));

        FriendCodeResponseDTO result = service.getOrIssueMyCode(userId);

        assertEquals("7351", result.getCode());
        verify(friendCodeRepository, times(1)).saveAndFlush(any(FriendCode.class));
    }

    @Test
    void 같은_자릿수에서_계속_충돌하면_자릿수를_늘려_발급한다() {
        when(friendCodeRepository.findByUserId(userId)).thenReturn(Optional.empty());
        // 4자리 20회 연속 충돌 후 5자리에서 성공
        var stubbing = when(friendCodeRepository.saveAndFlush(any(FriendCode.class)));
        for (int i = 0; i < 20; i++) {
            stubbing = stubbing.thenThrow(new DataIntegrityViolationException("uk_friend_code_code"));
        }
        stubbing.thenAnswer(inv -> inv.getArgument(0));

        FriendCodeResponseDTO result = service.getOrIssueMyCode(userId);

        assertEquals(5, result.getCode().length());
    }
}
