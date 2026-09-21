package com.umc.puppymode2.domain.social.service;

import com.umc.puppymode2.domain.cheer.repository.CheerRepository;
import com.umc.puppymode2.domain.friend.entity.enums.FriendRequestStatus;
import com.umc.puppymode2.domain.social.dto.SocialSummaryResponseDTO;
import com.umc.puppymode2.domain.social.repository.SocialSummaryRepository;
import com.umc.puppymode2.domain.user.entity.enums.UserStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SocialSummaryServiceImplTest {

    @Mock private SocialSummaryRepository socialSummaryRepository;
    @Mock private CheerRepository cheerRepository;

    @InjectMocks
    private SocialSummaryServiceImpl service;

    private static final Long ME = 10L;

    @Test
    void 받은_요청도_안_읽은_응원도_없으면_뱃지가_없다() {
        givenCounts(0, 0);

        SocialSummaryResponseDTO result = service.getSummary(ME);

        assertFalse(result.isHasBadge());
        assertEquals(0, result.getPendingRequestCount());
        assertEquals(0, result.getUnreadCheerCount());
    }

    @Test
    void 받은_친구_요청만_있어도_뱃지가_켜진다() {
        givenCounts(2, 0);

        SocialSummaryResponseDTO result = service.getSummary(ME);

        assertTrue(result.isHasBadge());
        assertEquals(2, result.getPendingRequestCount());
    }

    @Test
    void 안_읽은_응원만_있어도_뱃지가_켜진다() {
        givenCounts(0, 1);

        SocialSummaryResponseDTO result = service.getSummary(ME);

        assertTrue(result.isHasBadge());
        assertEquals(1, result.getUnreadCheerCount());
    }

    @Test
    void 둘_다_있으면_건수를_각각_그대로_준다() {
        givenCounts(2, 1);

        SocialSummaryResponseDTO result = service.getSummary(ME);

        assertTrue(result.isHasBadge());
        assertEquals(2, result.getPendingRequestCount());
        assertEquals(1, result.getUnreadCheerCount());
    }

    @Test
    void 요청은_PENDING_NORMAL_기준으로_센다() {
        givenCounts(0, 0);

        service.getSummary(ME);

        verify(socialSummaryRepository).countVisibleReceivedRequests(ME, FriendRequestStatus.PENDING, UserStatus.NORMAL);
        verify(cheerRepository).countUnreadReceivable(eq(ME), any(), eq(UserStatus.NORMAL));
    }

    private void givenCounts(long pendingRequests, long unreadCheers) {
        when(socialSummaryRepository.countVisibleReceivedRequests(eq(ME), any(), any())).thenReturn(pendingRequests);
        when(cheerRepository.countUnreadReceivable(eq(ME), any(), any())).thenReturn(unreadCheers);
    }
}
