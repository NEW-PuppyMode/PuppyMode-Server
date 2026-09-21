package com.umc.puppymode2.domain.cheer.service;

import com.umc.puppymode2.domain.cheer.converter.CheerConverter;
import com.umc.puppymode2.domain.cheer.dto.CheerSendRequestDTO;
import com.umc.puppymode2.domain.cheer.dto.CheerSendResponseDTO;
import com.umc.puppymode2.domain.cheer.entity.Cheer;
import com.umc.puppymode2.domain.cheer.entity.CheerTemplate;
import com.umc.puppymode2.domain.cheer.entity.enums.CheerCategory;
import com.umc.puppymode2.domain.cheer.exception.CheerErrorStatus;
import com.umc.puppymode2.domain.cheer.repository.CheerFriendshipRepository;
import com.umc.puppymode2.domain.cheer.repository.CheerRepository;
import com.umc.puppymode2.domain.cheer.repository.CheerTemplateRepository;
import com.umc.puppymode2.domain.friend.converter.FriendConverter;
import com.umc.puppymode2.domain.friend.entity.Friendship;
import com.umc.puppymode2.domain.friend.repository.FriendDrinkRecordProjection;
import com.umc.puppymode2.domain.friend.repository.FriendDrinkRecordRepository;
import com.umc.puppymode2.domain.user.auth.enums.Provider;
import com.umc.puppymode2.domain.user.entity.User;
import com.umc.puppymode2.domain.user.entity.enums.UserStatus;
import com.umc.puppymode2.domain.user.repository.UserRepository;
import com.umc.puppymode2.global.exception.GeneralException;
import com.umc.puppymode2.global.util.TimeConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CheerCommandServiceImplTest {

    @Mock private CheerRepository cheerRepository;
    @Mock private CheerTemplateRepository cheerTemplateRepository;
    @Mock private CheerFriendshipRepository cheerFriendshipRepository;
    @Mock private FriendDrinkRecordRepository friendDrinkRecordRepository;
    @Mock private UserRepository userRepository;
    @Spy private CheerTimePolicy timePolicy = new CheerTimePolicy();
    @Spy private CheerConverter converter = new CheerConverter(new FriendConverter());

    @InjectMocks
    private CheerCommandServiceImpl service;

    private static final Long ME = 10L;
    private static final Long FRIEND = 20L;
    private static final Long TEMPLATE_ID = 6L;

    private final LocalDate today = LocalDate.now(TimeConstants.KST);
    private final LocalDate yesterday = today.minusDays(1);

    // ---------------------------------------------------------------- 성공

    @Test
    void 오늘_음주한_친구에게_응원을_보낸다() {
        givenFriendAndTemplate();
        givenFriendDrank(today, true);
        givenSaveAssignsId(900L);

        CheerSendResponseDTO result = service.sendCheer(ME, FRIEND, request(today));

        assertEquals(900L, result.getCheerId());
        ArgumentCaptor<Cheer> captor = ArgumentCaptor.forClass(Cheer.class);
        verify(cheerRepository).saveAndFlush(captor.capture());
        Cheer saved = captor.getValue();
        assertEquals(ME, saved.getSenderId());
        assertEquals(FRIEND, saved.getReceiverId());
        assertEquals(today, saved.getTargetDate());
        assertNull(saved.getReadAt());
    }

    @Test
    void 문구와_분류는_발송_시점_스냅샷으로_저장된다() {
        givenFriendAndTemplate();
        givenFriendDrank(today, true);
        givenSaveAssignsId(900L);

        service.sendCheer(ME, FRIEND, request(today));

        ArgumentCaptor<Cheer> captor = ArgumentCaptor.forClass(Cheer.class);
        verify(cheerRepository).saveAndFlush(captor.capture());
        assertEquals(TEMPLATE_ID, captor.getValue().getCheerTemplateId());
        assertEquals(CheerCategory.COMFORT, captor.getValue().getCategory());
        assertEquals("그래 마실 수도 있지", captor.getValue().getMessageSnapshot());
    }

    @Test
    void 만료_시각은_오늘_더하기_2일_00시_KST이다() {
        givenFriendAndTemplate();
        givenFriendDrank(today, true);
        givenSaveAssignsId(900L);

        service.sendCheer(ME, FRIEND, request(today));

        ArgumentCaptor<Cheer> captor = ArgumentCaptor.forClass(Cheer.class);
        verify(cheerRepository).saveAndFlush(captor.capture());
        // 서버 타임존과 무관하게 "오늘+2일 KST 자정"이라는 같은 순간이어야 한다.
        ZonedDateTime expected = today.plusDays(2).atStartOfDay(TimeConstants.KST);
        assertEquals(expected.toInstant(),
                captor.getValue().getExpiresAt().atZone(ZoneId.systemDefault()).toInstant());
    }

    @Test
    void 어제_음주한_건도_응원할_수_있다() {
        givenFriendAndTemplate();
        givenFriendDrank(yesterday, true);
        givenSaveAssignsId(901L);

        CheerSendResponseDTO result = service.sendCheer(ME, FRIEND, request(yesterday));

        assertEquals(901L, result.getCheerId());
    }

    @Test
    void 이틀_연속_음주면_어제_건을_보낸_뒤에도_오늘_건을_보낼_수_있다() {
        givenFriendAndTemplate();
        givenFriendDrank(yesterday, true);
        givenFriendDrank(today, true);
        givenSaveAssignsId(900L);

        // 날짜가 다르면 (보낸 사람, 받는 사람, 날짜) 유니크에 걸리지 않으므로 둘 다 성공해야 한다.
        service.sendCheer(ME, FRIEND, request(yesterday));
        service.sendCheer(ME, FRIEND, request(today));

        ArgumentCaptor<Cheer> captor = ArgumentCaptor.forClass(Cheer.class);
        verify(cheerRepository, times(2)).saveAndFlush(captor.capture());
        assertEquals(List.of(yesterday, today),
                captor.getAllValues().stream().map(Cheer::getTargetDate).toList());
    }

    @Test
    void 응원을_보낸_뒤에는_친구_기록을_다시_확인하지_않아_기록이_수정돼도_응원은_유지된다() {
        givenFriendAndTemplate();
        givenFriendDrank(today, true);
        givenSaveAssignsId(900L);

        service.sendCheer(ME, FRIEND, request(today));

        // 음주 기록 검증은 발송 전에 딱 한 번만 한다. 발송 이후 is_drink가 false로 바뀌어도 보낸 응원을 지우거나 되돌리는 로직이 없다.
        verify(friendDrinkRecordRepository, times(1)).findRecords(anyCollection(), anyCollection());
        verify(cheerRepository, never()).delete(any(Cheer.class));
        verify(cheerRepository, never()).deleteAllBetween(any(), any());
    }

    // ---------------------------------------------------------------- 친구 관계 / 사용자 상태

    @Test
    void 친구_관계는_공유_잠금으로_조회한다() {
        // 확인과 저장 사이에 친구 삭제가 끼어들어 응원이 남는 경쟁 상태를 막기 위해, 관계 행을 잠그고 확인한다.
        givenFriendAndTemplate();
        givenFriendDrank(today, true);
        givenSaveAssignsId(900L);

        service.sendCheer(ME, FRIEND, request(today));

        verify(cheerFriendshipRepository).findWithSharedLock(ME, FRIEND);
    }

    @Test
    void 잠금_조회_시점에_이미_친구가_삭제됐다면_응원을_저장하지_않는다() {
        // 삭제가 먼저 끝난 경우: 잠금 조회가 삭제 완료를 기다린 뒤 빈 결과를 받는다.
        when(cheerFriendshipRepository.findWithSharedLock(ME, FRIEND)).thenReturn(Optional.empty());

        GeneralException e = assertThrows(GeneralException.class, () -> service.sendCheer(ME, FRIEND, request(today)));

        assertEquals(CheerErrorStatus.NOT_FRIENDS, e.getCode());
        verify(cheerRepository, never()).saveAndFlush(any());
    }

    @Test
    void 친구가_아니면_403() {
        when(cheerFriendshipRepository.findWithSharedLock(ME, FRIEND)).thenReturn(Optional.empty());

        GeneralException e = assertThrows(GeneralException.class, () -> service.sendCheer(ME, FRIEND, request(today)));

        assertEquals(CheerErrorStatus.NOT_FRIENDS, e.getCode());
        verifyNoInteractions(cheerTemplateRepository, friendDrinkRecordRepository);
    }

    @Test
    void 나_자신에게_보내면_친구_관계가_없으므로_403() {
        when(cheerFriendshipRepository.findWithSharedLock(ME, ME)).thenReturn(Optional.empty());

        GeneralException e = assertThrows(GeneralException.class, () -> service.sendCheer(ME, ME, request(today)));

        assertEquals(CheerErrorStatus.NOT_FRIENDS, e.getCode());
    }

    @Test
    void 친구의_ID_순서와_무관하게_low_high로_정규화해_친구를_판정한다() {
        Long bigMe = 99L;
        when(cheerFriendshipRepository.findWithSharedLock(FRIEND, bigMe)).thenReturn(Optional.empty());

        GeneralException e = assertThrows(GeneralException.class, () -> service.sendCheer(bigMe, FRIEND, request(today)));

        assertEquals(CheerErrorStatus.NOT_FRIENDS, e.getCode());
    }

    @Test
    void 친구가_NORMAL이_아니면_친구가_아닌_것과_같은_403() {
        when(cheerFriendshipRepository.findWithSharedLock(ME, FRIEND)).thenReturn(Optional.of(Friendship.of(ME, FRIEND)));
        when(userRepository.findById(FRIEND)).thenReturn(Optional.of(user(FRIEND, UserStatus.REST)));

        GeneralException e = assertThrows(GeneralException.class, () -> service.sendCheer(ME, FRIEND, request(today)));

        assertEquals(CheerErrorStatus.NOT_FRIENDS, e.getCode());
    }

    // ---------------------------------------------------------------- 문구

    @Test
    void 없거나_비활성인_문구는_404() {
        givenFriend();
        when(cheerTemplateRepository.findByCheerTemplateIdAndActiveTrue(TEMPLATE_ID)).thenReturn(Optional.empty());

        GeneralException e = assertThrows(GeneralException.class, () -> service.sendCheer(ME, FRIEND, request(today)));

        assertEquals(CheerErrorStatus.TEMPLATE_NOT_FOUND, e.getCode());
    }

    @Test
    void 문구와_날짜가_모두_잘못됐으면_문구_오류가_먼저다() {
        givenFriend();
        when(cheerTemplateRepository.findByCheerTemplateIdAndActiveTrue(TEMPLATE_ID)).thenReturn(Optional.empty());

        GeneralException e = assertThrows(GeneralException.class,
                () -> service.sendCheer(ME, FRIEND, request(today.minusDays(5))));

        assertEquals(CheerErrorStatus.TEMPLATE_NOT_FOUND, e.getCode());
    }

    // ---------------------------------------------------------------- 대상 날짜

    @Test
    void 그제_이전_날짜는_400() {
        givenFriendAndTemplate();

        GeneralException e = assertThrows(GeneralException.class,
                () -> service.sendCheer(ME, FRIEND, request(today.minusDays(2))));

        assertEquals(CheerErrorStatus.INVALID_TARGET_DATE, e.getCode());
        verify(cheerRepository, never()).saveAndFlush(any());
    }

    @Test
    void 미래_날짜는_400() {
        givenFriendAndTemplate();

        GeneralException e = assertThrows(GeneralException.class,
                () -> service.sendCheer(ME, FRIEND, request(today.plusDays(1))));

        assertEquals(CheerErrorStatus.INVALID_TARGET_DATE, e.getCode());
    }

    @Test
    void 그_날짜에_친구의_음주_기록이_없으면_400() {
        givenFriendAndTemplate();
        when(friendDrinkRecordRepository.findRecords(anyCollection(), anyCollection())).thenReturn(List.of());

        GeneralException e = assertThrows(GeneralException.class, () -> service.sendCheer(ME, FRIEND, request(today)));

        assertEquals(CheerErrorStatus.INVALID_TARGET_DATE, e.getCode());
    }

    @Test
    void 그_날짜_기록이_is_drink_false이면_마신_것이_아니므로_400() {
        givenFriendAndTemplate();
        givenFriendDrank(today, false);

        GeneralException e = assertThrows(GeneralException.class, () -> service.sendCheer(ME, FRIEND, request(today)));

        assertEquals(CheerErrorStatus.INVALID_TARGET_DATE, e.getCode());
    }

    @Test
    void 같은_날짜에_행이_여러_개여도_하나라도_true이면_응원할_수_있다() {
        givenFriendAndTemplate();
        when(friendDrinkRecordRepository.findRecords(anyCollection(), anyCollection()))
                .thenReturn(List.of(record(today, false), record(today, true)));
        givenSaveAssignsId(900L);

        assertDoesNotThrow(() -> service.sendCheer(ME, FRIEND, request(today)));
    }

    // ---------------------------------------------------------------- 중복 / 동시성

    @Test
    void 같은_날짜에_이미_보냈다면_409() {
        givenFriendAndTemplate();
        givenFriendDrank(today, true);
        when(cheerRepository.existsBySenderIdAndReceiverIdAndTargetDate(ME, FRIEND, today)).thenReturn(true);

        GeneralException e = assertThrows(GeneralException.class, () -> service.sendCheer(ME, FRIEND, request(today)));

        assertEquals(CheerErrorStatus.CHEER_ALREADY_SENT, e.getCode());
        verify(cheerRepository, never()).saveAndFlush(any());
    }

    @Test
    void 더블탭으로_동시에_들어와_UNIQUE_위반이면_409() {
        givenFriendAndTemplate();
        givenFriendDrank(today, true);
        when(cheerRepository.saveAndFlush(any(Cheer.class)))
                .thenThrow(new DataIntegrityViolationException("uk_cheer_once"));

        GeneralException e = assertThrows(GeneralException.class, () -> service.sendCheer(ME, FRIEND, request(today)));

        assertEquals(CheerErrorStatus.CHEER_ALREADY_SENT, e.getCode());
    }

    // ---------------------------------------------------------------- 읽음 처리

    @Test
    void 읽음_처리는_내가_받은_안_읽은_응원을_한꺼번에_갱신한다() {
        LocalDateTime before = LocalDateTime.now();

        service.markAllReceivedAsRead(ME);

        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(cheerRepository).markAllAsRead(eq(ME), captor.capture(), eq(UserStatus.NORMAL));
        assertFalse(captor.getValue().isBefore(before));
        assertFalse(captor.getValue().isAfter(LocalDateTime.now()));
    }

    @Test
    void 읽음_처리는_NORMAL_발신자의_응원만_대상으로_한다() {
        // 발신자가 휴면(REST)인 동안 목록/뱃지에 보이지 않는 응원이 읽음 처리되면,
        // 발신자가 복귀했을 때 목록에는 나타나는데 뱃지에서는 이미 읽은 것으로 빠져 어긋난다.
        service.markAllReceivedAsRead(ME);

        verify(cheerRepository).markAllAsRead(eq(ME), any(LocalDateTime.class), eq(UserStatus.NORMAL));
    }

    @Test
    void 읽을_응원이_없어도_예외_없이_성공한다() {
        when(cheerRepository.markAllAsRead(eq(ME), any(), any())).thenReturn(0);

        assertDoesNotThrow(() -> service.markAllReceivedAsRead(ME));
    }

    // ---------------------------------------------------------------- 픽스처

    private CheerSendRequestDTO request(LocalDate targetDate) {
        return new CheerSendRequestDTO(TEMPLATE_ID, targetDate);
    }

    private void givenFriend() {
        when(cheerFriendshipRepository.findWithSharedLock(ME, FRIEND)).thenReturn(Optional.of(Friendship.of(ME, FRIEND)));
        when(userRepository.findById(FRIEND)).thenReturn(Optional.of(user(FRIEND, UserStatus.NORMAL)));
    }

    private void givenFriendAndTemplate() {
        givenFriend();
        CheerTemplate template = CheerTemplate.of(CheerCategory.COMFORT, "그래 마실 수도 있지", 1);
        ReflectionTestUtils.setField(template, "cheerTemplateId", TEMPLATE_ID);
        lenient().when(cheerTemplateRepository.findByCheerTemplateIdAndActiveTrue(TEMPLATE_ID))
                .thenReturn(Optional.of(template));
    }

    // 날짜별로 친구의 음주 기록을 준비한다. (이틀 연속 시나리오에서 날짜마다 따로 호출)
    private void givenFriendDrank(LocalDate date, boolean isDrink) {
        when(friendDrinkRecordRepository.findRecords(anyCollection(), argThatContains(date)))
                .thenReturn(List.of(record(date, isDrink)));
    }

    private java.util.Collection<LocalDate> argThatContains(LocalDate date) {
        return org.mockito.ArgumentMatchers.argThat(dates -> dates != null && dates.contains(date));
    }

    private void givenSaveAssignsId(Long id) {
        when(cheerRepository.saveAndFlush(any(Cheer.class))).thenAnswer(inv -> {
            Cheer cheer = inv.getArgument(0);
            ReflectionTestUtils.setField(cheer, "cheerId", id);
            return cheer;
        });
    }

    private User user(Long userId, UserStatus status) {
        User user = User.builder()
                .username("user" + userId)
                .email("user" + userId + "@test.com")
                .provider(Provider.KAKAO)
                .status(status)
                .build();
        ReflectionTestUtils.setField(user, "userId", userId);
        return user;
    }

    private FriendDrinkRecordProjection record(LocalDate date, boolean isDrink) {
        return new FriendDrinkRecordProjection() {
            @Override public Long getUserId() { return FRIEND; }
            @Override public LocalDate getDrinkDate() { return date; }
            @Override public Boolean getIsDrink() { return isDrink; }
        };
    }
}
