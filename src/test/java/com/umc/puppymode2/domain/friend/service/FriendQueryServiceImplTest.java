package com.umc.puppymode2.domain.friend.service;

import com.umc.puppymode2.domain.friend.cache.PuppyProfileCache;
import com.umc.puppymode2.domain.friend.cache.PuppyProfileCache.PuppyProfile;
import com.umc.puppymode2.domain.friend.converter.FriendConverter;
import com.umc.puppymode2.domain.friend.dto.FriendCheerState;
import com.umc.puppymode2.domain.friend.dto.FriendDrinkStatus;
import com.umc.puppymode2.domain.friend.dto.FriendListResponseDTO;
import com.umc.puppymode2.domain.friend.dto.FriendProfileResponseDTO;
import com.umc.puppymode2.domain.friend.dto.ReceivedFriendRequestListResponseDTO;
import com.umc.puppymode2.domain.friend.entity.FriendRequest;
import com.umc.puppymode2.domain.friend.entity.enums.FriendRequestStatus;
import com.umc.puppymode2.domain.friend.exception.FriendErrorStatus;
import com.umc.puppymode2.domain.friend.repository.FriendDrinkRecordProjection;
import com.umc.puppymode2.domain.friend.repository.FriendDrinkRecordRepository;
import com.umc.puppymode2.domain.friend.repository.FriendRequestRepository;
import com.umc.puppymode2.domain.friend.repository.FriendshipRepository;
import com.umc.puppymode2.domain.puppy.entity.Puppy;
import com.umc.puppymode2.domain.puppy.entity.PuppyType;
import com.umc.puppymode2.domain.puppy.repository.PuppyRepository;
import com.umc.puppymode2.domain.user.auth.enums.Provider;
import com.umc.puppymode2.domain.user.entity.User;
import com.umc.puppymode2.domain.user.entity.enums.UserStatus;
import com.umc.puppymode2.domain.user.repository.UserRepository;
import com.umc.puppymode2.global.exception.GeneralException;
import com.umc.puppymode2.global.util.TimeConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FriendQueryServiceImplTest {

    @Mock private FriendRequestRepository friendRequestRepository;
    @Mock private FriendshipRepository friendshipRepository;
    @Mock private FriendDrinkRecordRepository friendDrinkRecordRepository;
    @Mock private UserRepository userRepository;
    @Mock private PuppyRepository puppyRepository;
    @Mock private PuppyProfileCache puppyProfileCache;
    @Spy private FriendStatusCalculator statusCalculator = new FriendStatusCalculator();
    @Spy private FriendConverter converter = new FriendConverter();

    @InjectMocks
    private FriendQueryServiceImpl service;

    private static final Long ME = 10L;

    private final LocalDate today = LocalDate.now(TimeConstants.KST);
    private final LocalDate yesterday = today.minusDays(1);

    @BeforeEach
    void setUp() {
        lenient().when(puppyProfileCache.profileOf(any())).thenReturn(new PuppyProfile(5, "https://cdn/img.png"));
    }

    // ---------------------------------------------------------------- 친구 목록

    @Test
    void 친구가_없으면_빈_목록을_반환한다() {
        when(friendshipRepository.findFriendIds(ME)).thenReturn(List.of());

        FriendListResponseDTO result = service.getFriends(ME);

        assertEquals(0, result.getCount());
        assertTrue(result.getFriends().isEmpty());
        verifyNoInteractions(userRepository, friendDrinkRecordRepository);
    }

    @Test
    void 친구_목록은_가나다순으로_정렬된다() {
        when(friendshipRepository.findFriendIds(ME)).thenReturn(List.of(1L, 2L, 3L, 4L));
        when(userRepository.findAllById(anyCollection())).thenReturn(List.of(
                user(1L, "최짱짱", UserStatus.NORMAL),
                user(2L, "김뭉뭉", UserStatus.NORMAL),
                user(3L, "박쭝쭝", UserStatus.NORMAL),
                user(4L, "가나다", UserStatus.NORMAL)));
        when(puppyRepository.findAllByUserUserIdIn(any())).thenReturn(List.of());
        when(friendDrinkRecordRepository.findRecords(anyCollection(), anyCollection())).thenReturn(List.of());

        FriendListResponseDTO result = service.getFriends(ME);

        assertEquals(List.of("가나다", "김뭉뭉", "박쭝쭝", "최짱짱"),
                result.getFriends().stream().map(FriendListResponseDTO.Item::getUsername).toList());
        assertEquals(4, result.getCount());
    }

    @Test
    void NORMAL이_아닌_친구는_목록에서_제외된다() {
        when(friendshipRepository.findFriendIds(ME)).thenReturn(List.of(1L, 2L, 3L));
        when(userRepository.findAllById(anyCollection())).thenReturn(List.of(
                user(1L, "정상", UserStatus.NORMAL),
                user(2L, "휴면", UserStatus.REST),
                user(3L, "탈퇴", UserStatus.STOP)));
        when(puppyRepository.findAllByUserUserIdIn(any())).thenReturn(List.of());
        when(friendDrinkRecordRepository.findRecords(anyCollection(), anyCollection())).thenReturn(List.of());

        FriendListResponseDTO result = service.getFriends(ME);

        assertEquals(1, result.getCount());
        assertEquals(1L, result.getFriends().get(0).getUserId());
    }

    @Test
    void 친구별_음주_상태와_응원_버튼_상태가_계산된다() {
        when(friendshipRepository.findFriendIds(ME)).thenReturn(List.of(1L, 2L, 3L, 4L));
        when(userRepository.findAllById(anyCollection())).thenReturn(List.of(
                user(1L, "가", UserStatus.NORMAL),  // 오늘 음주
                user(2L, "나", UserStatus.NORMAL),  // 어제 음주
                user(3L, "다", UserStatus.NORMAL),  // 오늘 기록 있는데 마시지 않음
                user(4L, "라", UserStatus.NORMAL))); // 기록 없음
        when(puppyRepository.findAllByUserUserIdIn(any())).thenReturn(List.of());
        when(friendDrinkRecordRepository.findRecords(anyCollection(), anyCollection())).thenReturn(List.of(
                record(1L, today, true),
                record(2L, yesterday, true),
                record(3L, today, false)));

        List<FriendListResponseDTO.Item> friends = service.getFriends(ME).getFriends();

        assertEquals(FriendDrinkStatus.DRANK_TODAY, friends.get(0).getDrinkStatus());
        assertEquals(FriendCheerState.ACTIVE, friends.get(0).getCheer().getState());
        assertEquals(today, friends.get(0).getCheer().getTargetDate());

        assertEquals(FriendDrinkStatus.DRANK_YESTERDAY, friends.get(1).getDrinkStatus());
        assertEquals(yesterday, friends.get(1).getCheer().getTargetDate());

        assertEquals(FriendDrinkStatus.NOT_DRANK, friends.get(2).getDrinkStatus());
        assertEquals(FriendCheerState.DISABLED, friends.get(2).getCheer().getState());
        assertNull(friends.get(2).getCheer().getTargetDate());

        assertEquals(FriendDrinkStatus.NONE, friends.get(3).getDrinkStatus());
        assertEquals(FriendCheerState.DISABLED, friends.get(3).getCheer().getState());
    }

    @Test
    void 같은_날짜에_음주_행이_여러_개여도_하나라도_true면_마신_것으로_본다() {
        // DrinkHistory에 (user_id, drink_date) 유니크가 없어 같은 날 행이 여러 개일 수 있다.
        when(friendshipRepository.findFriendIds(ME)).thenReturn(List.of(1L));
        when(userRepository.findAllById(anyCollection())).thenReturn(List.of(user(1L, "가", UserStatus.NORMAL)));
        when(puppyRepository.findAllByUserUserIdIn(any())).thenReturn(List.of());
        when(friendDrinkRecordRepository.findRecords(anyCollection(), anyCollection())).thenReturn(List.of(
                record(1L, today, false),
                record(1L, today, true)));

        FriendListResponseDTO.Item friend = service.getFriends(ME).getFriends().get(0);

        assertEquals(FriendDrinkStatus.DRANK_TODAY, friend.getDrinkStatus());
    }

    @Test
    void 강아지_정보가_있으면_강아지_이름과_레벨_이미지가_채워진다() {
        Puppy puppy = Puppy.builder().puppyType(PuppyType.CORGI).puppyName("쿠키").puppyExp(100).build();
        User friend = user(1L, "가", UserStatus.NORMAL);
        ReflectionTestUtils.setField(puppy, "user", friend);
        when(friendshipRepository.findFriendIds(ME)).thenReturn(List.of(1L));
        when(userRepository.findAllById(anyCollection())).thenReturn(List.of(friend));
        when(puppyRepository.findAllByUserUserIdIn(any())).thenReturn(List.of(puppy));
        when(friendDrinkRecordRepository.findRecords(anyCollection(), anyCollection())).thenReturn(List.of());

        FriendListResponseDTO.Item item = service.getFriends(ME).getFriends().get(0);

        assertEquals("쿠키", item.getPuppyName());
        assertEquals(5, item.getLevel());
        assertEquals("https://cdn/img.png", item.getProfileImageUrl());
    }

    // ---------------------------------------------------------------- 받은 요청 목록

    @Test
    void 받은_요청이_없으면_빈_목록() {
        when(friendRequestRepository.findAllByReceiverAndStatus(ME, FriendRequestStatus.PENDING)).thenReturn(List.of());

        ReceivedFriendRequestListResponseDTO result = service.getReceivedRequests(ME);

        assertEquals(0, result.getCount());
        verifyNoInteractions(userRepository);
    }

    @Test
    void 받은_요청_목록은_요청_순서를_유지하고_NORMAL이_아닌_요청자는_제외한다() {
        when(friendRequestRepository.findAllByReceiverAndStatus(ME, FriendRequestStatus.PENDING)).thenReturn(List.of(
                request(103L, 3L, ME), request(102L, 2L, ME), request(101L, 1L, ME)));
        when(friendshipRepository.findFriendIds(ME)).thenReturn(List.of());
        when(userRepository.findAllById(anyCollection())).thenReturn(List.of(
                user(1L, "가", UserStatus.NORMAL),
                user(2L, "탈퇴", UserStatus.STOP),
                user(3L, "다", UserStatus.NORMAL)));
        when(puppyRepository.findAllByUserUserIdIn(any())).thenReturn(List.of());

        ReceivedFriendRequestListResponseDTO result = service.getReceivedRequests(ME);

        assertEquals(2, result.getCount());
        assertEquals(List.of(103L, 101L),
                result.getRequests().stream().map(ReceivedFriendRequestListResponseDTO.Item::getRequestId).toList());
    }

    @Test
    void 이미_친구가_된_사용자의_남은_PENDING_요청은_목록에서_제외한다() {
        // 서로 동시에 요청해 반대 방향 PENDING이 남은 경우
        when(friendRequestRepository.findAllByReceiverAndStatus(ME, FriendRequestStatus.PENDING))
                .thenReturn(List.of(request(101L, 1L, ME)));
        when(friendshipRepository.findFriendIds(ME)).thenReturn(List.of(1L));

        ReceivedFriendRequestListResponseDTO result = service.getReceivedRequests(ME);

        assertEquals(0, result.getCount());
    }

    // ---------------------------------------------------------------- 친구 프로필

    @Test
    void 친구가_아니면_프로필_조회는_403() {
        when(friendshipRepository.existsByUserLowIdAndUserHighId(1L, ME)).thenReturn(false);

        GeneralException e = assertThrows(GeneralException.class, () -> service.getFriendProfile(ME, 1L));

        assertEquals(FriendErrorStatus.NOT_FRIENDS, e.getCode());
        verifyNoInteractions(userRepository);
    }

    @Test
    void 친구가_비활성_상태면_프로필_조회는_404() {
        when(friendshipRepository.existsByUserLowIdAndUserHighId(1L, ME)).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, "휴면", UserStatus.REST)));

        GeneralException e = assertThrows(GeneralException.class, () -> service.getFriendProfile(ME, 1L));

        assertEquals(FriendErrorStatus.FRIEND_USER_NOT_FOUND, e.getCode());
    }

    @Test
    void 친구_프로필을_조회한다() {
        Puppy puppy = Puppy.builder().puppyType(PuppyType.BICHON).puppyName("콩이").puppyExp(10).build();
        when(friendshipRepository.existsByUserLowIdAndUserHighId(1L, ME)).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, "이왈왈", UserStatus.NORMAL)));
        when(puppyRepository.findByUser_UserId(1L)).thenReturn(Optional.of(puppy));

        FriendProfileResponseDTO result = service.getFriendProfile(ME, 1L);

        assertEquals("이왈왈", result.getUsername());
        assertEquals("콩이", result.getPuppyName());
        assertEquals(PuppyType.BICHON, result.getPuppyType());
        assertEquals(5, result.getLevel());
    }

    // ---------------------------------------------------------------- 픽스처

    private User user(Long userId, String username, UserStatus status) {
        User user = User.builder()
                .username(username)
                .email("user" + userId + "@test.com")
                .provider(Provider.KAKAO)
                .status(status)
                .build();
        ReflectionTestUtils.setField(user, "userId", userId);
        return user;
    }

    private FriendRequest request(Long id, Long requesterId, Long receiverId) {
        FriendRequest request = FriendRequest.create(requesterId, receiverId);
        ReflectionTestUtils.setField(request, "friendRequestId", id);
        return request;
    }

    private FriendDrinkRecordProjection record(Long userId, LocalDate date, boolean isDrink) {
        return new FriendDrinkRecordProjection() {
            @Override public Long getUserId() { return userId; }
            @Override public LocalDate getDrinkDate() { return date; }
            @Override public Boolean getIsDrink() { return isDrink; }
        };
    }
}
