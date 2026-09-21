package com.umc.puppymode2.domain.social.service;

import com.umc.puppymode2.domain.cheer.repository.CheerRepository;
import com.umc.puppymode2.domain.friend.entity.enums.FriendRequestStatus;
import com.umc.puppymode2.domain.social.dto.SocialSummaryResponseDTO;
import com.umc.puppymode2.domain.social.repository.SocialSummaryRepository;
import com.umc.puppymode2.domain.user.entity.enums.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SocialSummaryServiceImpl implements SocialSummaryService {

    private final SocialSummaryRepository socialSummaryRepository;
    private final CheerRepository cheerRepository;

    @Override
    public SocialSummaryResponseDTO getSummary(Long myUserId) {
        long pendingRequestCount = socialSummaryRepository.countVisibleReceivedRequests(
                myUserId, FriendRequestStatus.PENDING, UserStatus.NORMAL);

        // 만료 시각은 JVM 기본 타임존 기준으로 저장되므로 비교 시각도 같은 기준으로 만든다.
        long unreadCheerCount = cheerRepository.countUnreadReceivable(
                myUserId, LocalDateTime.now(), UserStatus.NORMAL);

        return SocialSummaryResponseDTO.builder()
                .hasBadge(pendingRequestCount > 0 || unreadCheerCount > 0)
                .pendingRequestCount(pendingRequestCount)
                .unreadCheerCount(unreadCheerCount)
                .build();
    }
}
