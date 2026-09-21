package com.umc.puppymode2.domain.friend.converter;

import com.umc.puppymode2.domain.friend.cache.PuppyProfileCache.PuppyProfile;
import com.umc.puppymode2.domain.friend.dto.*;
import com.umc.puppymode2.domain.friend.entity.FriendRequest;
import com.umc.puppymode2.domain.friend.entity.enums.FriendRequestStatus;
import com.umc.puppymode2.domain.friend.service.FriendStatusCalculator.FriendStatus;
import com.umc.puppymode2.domain.puppy.entity.Puppy;
import com.umc.puppymode2.domain.user.entity.User;
import com.umc.puppymode2.global.util.TimeConstants;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

@Component
public class FriendConverter {

    public FriendCodeResponseDTO toCodeDto(String code) {
        return FriendCodeResponseDTO.builder().code(code).build();
    }

    public FriendRequestSendResponseDTO toSendDto(FriendRequest request, boolean autoAccepted) {
        return FriendRequestSendResponseDTO.builder()
                .requestId(request.getFriendRequestId())
                .status(autoAccepted ? FriendRequestStatus.ACCEPTED : request.getStatus())
                .autoAccepted(autoAccepted)
                .build();
    }

    public FriendRequestAcceptResponseDTO toAcceptDto(Long friendUserId) {
        return FriendRequestAcceptResponseDTO.builder().friendUserId(friendUserId).build();
    }

    public ReceivedFriendRequestListResponseDTO.Item toReceivedItem(
            FriendRequest request, User requester, Puppy puppy, PuppyProfile profile) {
        return ReceivedFriendRequestListResponseDTO.Item.builder()
                .requestId(request.getFriendRequestId())
                .userId(requester.getUserId())
                .username(requester.getUsername())
                .puppyName(puppy == null ? null : puppy.getPuppyName())
                .level(profile.level())
                .profileImageUrl(profile.imageUrl())
                .requestedAt(toKstOffset(request.getCreatedAt()))
                .build();
    }

    public ReceivedFriendRequestListResponseDTO toReceivedDto(List<ReceivedFriendRequestListResponseDTO.Item> items) {
        return ReceivedFriendRequestListResponseDTO.builder()
                .count(items.size())
                .requests(items)
                .build();
    }

    public FriendListResponseDTO.Item toFriendItem(User friend, Puppy puppy, PuppyProfile profile, FriendStatus status) {
        return FriendListResponseDTO.Item.builder()
                .userId(friend.getUserId())
                .username(friend.getUsername())
                .puppyName(puppy == null ? null : puppy.getPuppyName())
                .level(profile.level())
                .profileImageUrl(profile.imageUrl())
                .drinkStatus(status.drinkStatus())
                .cheer(FriendListResponseDTO.Cheer.builder()
                        .state(status.cheerState())
                        .targetDate(status.cheerTargetDate())
                        .build())
                .build();
    }

    public FriendListResponseDTO toFriendListDto(List<FriendListResponseDTO.Item> items) {
        return FriendListResponseDTO.builder()
                .count(items.size())
                .friends(items)
                .build();
    }

    public FriendProfileResponseDTO toProfileDto(User friend, Puppy puppy, PuppyProfile profile) {
        return FriendProfileResponseDTO.builder()
                .userId(friend.getUserId())
                .username(friend.getUsername())
                .puppyName(puppy == null ? null : puppy.getPuppyName())
                .puppyType(puppy == null ? null : puppy.getPuppyType())
                .level(profile.level())
                .profileImageUrl(profile.imageUrl())
                .build();
    }

    // BaseEntity의 createdAt은 JPA Auditing이 서버(JVM) 기본 타임존으로 채운 LocalDateTime이다.
    // 서버 타임존이 UTC든 KST든 같은 시각을 가리키도록, 기본 타임존으로 해석한 뒤 KST(+09:00)로 변환한다.
    private OffsetDateTime toKstOffset(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.atZone(ZoneId.systemDefault())
                .withZoneSameInstant(TimeConstants.KST)
                .toOffsetDateTime();
    }
}
