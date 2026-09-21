package com.umc.puppymode2.domain.cheer.service;

import com.umc.puppymode2.domain.cheer.converter.CheerConverter;
import com.umc.puppymode2.domain.cheer.dto.CheerExpiresIn;
import com.umc.puppymode2.domain.cheer.dto.ReceivedCheerListResponseDTO;
import com.umc.puppymode2.domain.cheer.entity.Cheer;
import com.umc.puppymode2.domain.cheer.entity.CheerTemplate;
import com.umc.puppymode2.domain.cheer.entity.enums.CheerCategory;
import com.umc.puppymode2.domain.cheer.repository.CheerRepository;
import com.umc.puppymode2.domain.friend.cache.PuppyProfileCache;
import com.umc.puppymode2.domain.friend.cache.PuppyProfileCache.PuppyProfile;
import com.umc.puppymode2.domain.friend.converter.FriendConverter;
import com.umc.puppymode2.domain.puppy.repository.PuppyRepository;
import com.umc.puppymode2.domain.user.auth.enums.Provider;
import com.umc.puppymode2.domain.user.entity.User;
import com.umc.puppymode2.domain.user.entity.enums.UserStatus;
import com.umc.puppymode2.domain.user.repository.UserRepository;
import com.umc.puppymode2.global.util.TimeConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CheerQueryServiceImplTest {

    @Mock private CheerRepository cheerRepository;
    @Mock private UserRepository userRepository;
    @Mock private PuppyRepository puppyRepository;
    @Mock private PuppyProfileCache puppyProfileCache;
    @Spy private CheerTimePolicy timePolicy = new CheerTimePolicy();
    @Spy private CheerConverter converter = new CheerConverter(new FriendConverter());

    @InjectMocks
    private CheerQueryServiceImpl service;

    private static final Long ME = 10L;

    private TimeZone originalTimeZone;

    @BeforeEach
    void setUp() {
        originalTimeZone = TimeZone.getDefault();
        lenient().when(puppyProfileCache.profileOf(any())).thenReturn(new PuppyProfile(5, "https://cdn/img.png"));
    }

    @AfterEach
    void restoreTimeZone() {
        TimeZone.setDefault(originalTimeZone);
    }

    @Test
    void 받은_응원이_없으면_빈_목록() {
        when(cheerRepository.findReceivable(eq(ME), any(), any())).thenReturn(List.of());

        ReceivedCheerListResponseDTO result = service.getReceivedCheers(ME);

        assertEquals(0, result.getCount());
        assertTrue(result.getCheers().isEmpty());
        verifyNoInteractions(userRepository);
    }

    @Test
    void 받은_응원_목록은_조회된_순서를_유지하고_건수를_함께_준다() {
        when(cheerRepository.findReceivable(eq(ME), any(), any())).thenReturn(List.of(
                cheer(900L, 31L, "마신 건 마신거고 다음이 중요해", CheerCategory.CHEER, null),
                cheer(890L, 12L, "이정도면 알콜 중독이야", CheerCategory.PRANK, LocalDateTime.now())));
        when(userRepository.findAllById(anyCollection())).thenReturn(List.of(
                user(31L, "박쭝쭝"), user(12L, "김뭉뭉")));
        when(puppyRepository.findAllByUserUserIdIn(any())).thenReturn(List.of());

        ReceivedCheerListResponseDTO result = service.getReceivedCheers(ME);

        assertEquals(2, result.getCount());
        assertEquals(List.of(900L, 890L),
                result.getCheers().stream().map(ReceivedCheerListResponseDTO.Item::getCheerId).toList());
    }

    @Test
    void 응원_항목에_보낸_사람과_발송_시점_문구_스냅샷이_담긴다() {
        when(cheerRepository.findReceivable(eq(ME), any(), any())).thenReturn(List.of(
                cheer(900L, 31L, "다 이유가 있겠지", CheerCategory.COMFORT, null)));
        when(userRepository.findAllById(anyCollection())).thenReturn(List.of(user(31L, "박쭝쭝")));
        when(puppyRepository.findAllByUserUserIdIn(any())).thenReturn(List.of());

        ReceivedCheerListResponseDTO.Item item = service.getReceivedCheers(ME).getCheers().get(0);

        assertEquals(31L, item.getSender().getUserId());
        assertEquals("박쭝쭝", item.getSender().getUsername());
        assertEquals("https://cdn/img.png", item.getSender().getProfileImageUrl());
        assertEquals(CheerCategory.COMFORT, item.getCategory());
        assertEquals("다 이유가 있겠지", item.getMessage());
    }

    @Test
    void 읽음_여부가_그대로_내려간다() {
        when(cheerRepository.findReceivable(eq(ME), any(), any())).thenReturn(List.of(
                cheer(2L, 31L, "a", CheerCategory.CHEER, null),
                cheer(1L, 31L, "b", CheerCategory.CHEER, LocalDateTime.now())));
        when(userRepository.findAllById(anyCollection())).thenReturn(List.of(user(31L, "박쭝쭝")));
        when(puppyRepository.findAllByUserUserIdIn(any())).thenReturn(List.of());

        List<ReceivedCheerListResponseDTO.Item> items = service.getReceivedCheers(ME).getCheers();

        assertFalse(items.get(0).getIsRead());
        assertTrue(items.get(1).getIsRead());
    }

    @Test
    void 만료_시각에_따라_expiresIn이_TODAY_또는_TOMORROW로_내려간다() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
        LocalDate todayKst = LocalDate.now(TimeConstants.KST);
        // 오늘 받은 응원: 모레 00시 만료 → 내일 밤에 사라짐(TOMORROW)
        Cheer receivedToday = cheer(2L, 31L, "a", CheerCategory.CHEER, null,
                todayKst.plusDays(2).atStartOfDay());
        // 어제 받은 응원: 내일 00시 만료 → 오늘 밤에 사라짐(TODAY)
        Cheer receivedYesterday = cheer(1L, 31L, "b", CheerCategory.CHEER, null,
                todayKst.plusDays(1).atStartOfDay());
        when(cheerRepository.findReceivable(eq(ME), any(), any())).thenReturn(List.of(receivedToday, receivedYesterday));
        when(userRepository.findAllById(anyCollection())).thenReturn(List.of(user(31L, "박쭝쭝")));
        when(puppyRepository.findAllByUserUserIdIn(any())).thenReturn(List.of());

        List<ReceivedCheerListResponseDTO.Item> items = service.getReceivedCheers(ME).getCheers();

        assertEquals(CheerExpiresIn.TOMORROW, items.get(0).getExpiresIn());
        assertEquals(CheerExpiresIn.TODAY, items.get(1).getExpiresIn());
    }

    @Test
    void 응답_시각은_서버_타임존과_무관하게_KST_오프셋으로_내려간다() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        // KST 2026-09-23 00:00 만료 = UTC 표현으로는 09-22 15:00
        Cheer cheer = cheer(1L, 31L, "a", CheerCategory.CHEER, null, LocalDateTime.of(2026, 9, 22, 15, 0));
        ReflectionTestUtils.setField(cheer, "createdAt", LocalDateTime.of(2026, 9, 21, 12, 30));
        when(cheerRepository.findReceivable(eq(ME), any(), any())).thenReturn(List.of(cheer));
        when(userRepository.findAllById(anyCollection())).thenReturn(List.of(user(31L, "박쭝쭝")));
        when(puppyRepository.findAllByUserUserIdIn(any())).thenReturn(List.of());

        ReceivedCheerListResponseDTO.Item item = service.getReceivedCheers(ME).getCheers().get(0);

        assertEquals("2026-09-23T00:00+09:00", item.getExpiresAt().toString());
        assertEquals("2026-09-21T21:30+09:00", item.getReceivedAt().toString());
    }

    @Test
    void 조회_시점_기준으로_만료_전_응원만_요청하고_NORMAL_발신자로_한정한다() {
        when(cheerRepository.findReceivable(eq(ME), any(), any())).thenReturn(List.of());
        LocalDateTime before = LocalDateTime.now();

        service.getReceivedCheers(ME);

        var nowCaptor = org.mockito.ArgumentCaptor.forClass(LocalDateTime.class);
        var statusCaptor = org.mockito.ArgumentCaptor.forClass(UserStatus.class);
        verify(cheerRepository).findReceivable(eq(ME), nowCaptor.capture(), statusCaptor.capture());
        assertFalse(nowCaptor.getValue().isBefore(before));
        assertEquals(UserStatus.NORMAL, statusCaptor.getValue());
    }

    @Test
    void 발신자_정보를_찾을_수_없는_응원은_목록에서_제외한다() {
        when(cheerRepository.findReceivable(eq(ME), any(), any())).thenReturn(List.of(
                cheer(2L, 99L, "a", CheerCategory.CHEER, null),
                cheer(1L, 31L, "b", CheerCategory.CHEER, null)));
        when(userRepository.findAllById(anyCollection())).thenReturn(List.of(user(31L, "박쭝쭝")));
        when(puppyRepository.findAllByUserUserIdIn(any())).thenReturn(List.of());

        ReceivedCheerListResponseDTO result = service.getReceivedCheers(ME);

        assertEquals(1, result.getCount());
        assertEquals(1L, result.getCheers().get(0).getCheerId());
    }

    // ---------------------------------------------------------------- 픽스처

    private Cheer cheer(Long id, Long senderId, String message, CheerCategory category, LocalDateTime readAt) {
        return cheer(id, senderId, message, category, readAt, LocalDateTime.now().plusDays(1));
    }

    private Cheer cheer(Long id, Long senderId, String message, CheerCategory category,
                        LocalDateTime readAt, LocalDateTime expiresAt) {
        CheerTemplate template = CheerTemplate.of(category, message, 1);
        ReflectionTestUtils.setField(template, "cheerTemplateId", 6L);
        Cheer cheer = Cheer.of(senderId, ME, template, LocalDate.now(TimeConstants.KST), expiresAt);
        ReflectionTestUtils.setField(cheer, "cheerId", id);
        ReflectionTestUtils.setField(cheer, "readAt", readAt);
        ReflectionTestUtils.setField(cheer, "createdAt", LocalDateTime.now(ZoneId.systemDefault()));
        return cheer;
    }

    private User user(Long userId, String username) {
        User user = User.builder()
                .username(username)
                .email("user" + userId + "@test.com")
                .provider(Provider.KAKAO)
                .status(UserStatus.NORMAL)
                .build();
        ReflectionTestUtils.setField(user, "userId", userId);
        return user;
    }
}
