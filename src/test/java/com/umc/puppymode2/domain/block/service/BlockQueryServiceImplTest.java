package com.umc.puppymode2.domain.block.service;

import com.umc.puppymode2.domain.block.dto.BlockListResponseDTO;
import com.umc.puppymode2.domain.block.entity.UserBlock;
import com.umc.puppymode2.domain.block.repository.UserBlockRepository;
import com.umc.puppymode2.domain.friend.cache.PuppyProfileCache;
import com.umc.puppymode2.domain.friend.cache.PuppyProfileCache.PuppyProfile;
import com.umc.puppymode2.domain.friend.converter.FriendConverter;
import com.umc.puppymode2.domain.puppy.entity.Puppy;
import com.umc.puppymode2.domain.puppy.repository.PuppyRepository;
import com.umc.puppymode2.domain.user.auth.enums.Provider;
import com.umc.puppymode2.domain.user.entity.User;
import com.umc.puppymode2.domain.user.entity.enums.UserStatus;
import com.umc.puppymode2.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BlockQueryServiceImplTest {

    @Mock private UserBlockRepository userBlockRepository;
    @Mock private UserRepository userRepository;
    @Mock private PuppyRepository puppyRepository;
    @Mock private PuppyProfileCache puppyProfileCache;
    @Spy private FriendConverter friendConverter = new FriendConverter();

    @InjectMocks
    private BlockQueryServiceImpl service;

    private static final Long ME = 10L;

    private User user(Long id, String name, UserStatus status) {
        User user = User.builder()
                .username(name)
                .email("user" + id + "@test.com")
                .provider(Provider.KAKAO)
                .status(status)
                .build();
        ReflectionTestUtils.setField(user, "userId", id);
        return user;
    }

    private UserBlock block(Long blockedId, LocalDateTime createdAt) {
        UserBlock block = UserBlock.of(ME, blockedId);
        ReflectionTestUtils.setField(block, "createdAt", createdAt);
        return block;
    }

    private Puppy puppy(User owner, String name) {
        Puppy puppy = Puppy.builder().puppyName(name).build();
        ReflectionTestUtils.setField(puppy, "user", owner);
        return puppy;
    }

    @Test
    void 차단한_사람이_없으면_빈_목록() {
        when(userBlockRepository.findAllByBlocker(ME)).thenReturn(List.of());

        BlockListResponseDTO result = service.getBlocks(ME);

        assertEquals(0, result.getCount());
        assertTrue(result.getBlocks().isEmpty());
        verifyNoInteractions(userRepository);
    }

    @Test
    void 저장소가_준_최근_차단순_순서를_유지하고_강아지_정보를_함께_내려준다() {
        User first = user(1L, "가", UserStatus.NORMAL);
        User second = user(2L, "나", UserStatus.NORMAL);
        when(userBlockRepository.findAllByBlocker(ME)).thenReturn(List.of(
                block(2L, LocalDateTime.of(2026, 10, 7, 12, 0)),
                block(1L, LocalDateTime.of(2026, 10, 7, 10, 0))));
        when(userRepository.findAllById(anyCollection())).thenReturn(List.of(first, second));
        when(puppyRepository.findAllByUserUserIdIn(any())).thenReturn(List.of(puppy(second, "별이")));
        when(puppyProfileCache.profileOf(any())).thenReturn(new PuppyProfile(null, null));

        BlockListResponseDTO result = service.getBlocks(ME);

        assertEquals(2, result.getCount());
        assertEquals(List.of(2L, 1L), result.getBlocks().stream().map(BlockListResponseDTO.Item::getUserId).toList());
        assertEquals("별이", result.getBlocks().get(0).getPuppyName());
        assertNull(result.getBlocks().get(1).getPuppyName());
        assertNotNull(result.getBlocks().get(0).getBlockedAt());
        assertEquals(9 * 3600, result.getBlocks().get(0).getBlockedAt().getOffset().getTotalSeconds());
    }

    @Test
    void 탈퇴_휴면_사용자는_목록에서_제외하고_개수도_맞춘다() {
        when(userBlockRepository.findAllByBlocker(ME)).thenReturn(List.of(
                block(1L, LocalDateTime.of(2026, 10, 7, 12, 0)),
                block(2L, LocalDateTime.of(2026, 10, 7, 11, 0)),
                block(3L, LocalDateTime.of(2026, 10, 7, 10, 0))));
        when(userRepository.findAllById(anyCollection())).thenReturn(List.of(
                user(1L, "정상", UserStatus.NORMAL),
                user(2L, "휴면", UserStatus.REST),
                user(3L, "탈퇴", UserStatus.STOP)));
        when(puppyRepository.findAllByUserUserIdIn(any())).thenReturn(List.of());
        when(puppyProfileCache.profileOf(any())).thenReturn(new PuppyProfile(null, null));

        BlockListResponseDTO result = service.getBlocks(ME);

        assertEquals(1, result.getCount());
        assertEquals(1L, result.getBlocks().get(0).getUserId());
    }
}
