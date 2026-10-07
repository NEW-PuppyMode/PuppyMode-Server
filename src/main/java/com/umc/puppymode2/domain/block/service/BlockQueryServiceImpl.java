package com.umc.puppymode2.domain.block.service;

import com.umc.puppymode2.domain.block.dto.BlockListResponseDTO;
import com.umc.puppymode2.domain.block.entity.UserBlock;
import com.umc.puppymode2.domain.block.repository.UserBlockRepository;
import com.umc.puppymode2.domain.friend.cache.PuppyProfileCache;
import com.umc.puppymode2.domain.friend.cache.PuppyProfileCache.PuppyProfile;
import com.umc.puppymode2.domain.friend.converter.FriendConverter;
import com.umc.puppymode2.domain.puppy.entity.Puppy;
import com.umc.puppymode2.domain.puppy.repository.PuppyRepository;
import com.umc.puppymode2.domain.user.entity.User;
import com.umc.puppymode2.domain.user.entity.enums.UserStatus;
import com.umc.puppymode2.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BlockQueryServiceImpl implements BlockQueryService {

    private final UserBlockRepository userBlockRepository;
    private final UserRepository userRepository;
    private final PuppyRepository puppyRepository;
    private final PuppyProfileCache puppyProfileCache;
    private final FriendConverter friendConverter;

    @Transactional(readOnly = true)
    @Override
    public BlockListResponseDTO getBlocks(Long myUserId) {
        List<UserBlock> blocks = userBlockRepository.findAllByBlocker(myUserId);
        if (blocks.isEmpty()) {
            return toDto(List.of());
        }

        List<Long> blockedIds = blocks.stream().map(UserBlock::getBlockedId).distinct().toList();

        // 차단당한 사용자가 NORMAL이 아니면(탈퇴/휴면) 목록에서 제외한다.
        Map<Long, User> users = userRepository.findAllById(blockedIds).stream()
                .filter(user -> user.getStatus() == UserStatus.NORMAL)
                .collect(Collectors.toMap(User::getUserId, Function.identity()));
        Map<Long, Puppy> puppies = puppyRepository.findAllByUserUserIdIn(new ArrayList<>(users.keySet())).stream()
                .collect(Collectors.toMap(p -> p.getUser().getUserId(), Function.identity(), (a, b) -> a));

        List<BlockListResponseDTO.Item> items = new ArrayList<>();
        for (UserBlock block : blocks) {
            User blocked = users.get(block.getBlockedId());
            if (blocked == null) {
                continue;
            }
            Puppy puppy = puppies.get(blocked.getUserId());
            PuppyProfile profile = puppyProfileCache.profileOf(puppy);
            items.add(BlockListResponseDTO.Item.builder()
                    .userId(blocked.getUserId())
                    .username(blocked.getUsername())
                    .puppyName(puppy == null ? null : puppy.getPuppyName())
                    .level(profile.level())
                    .profileImageUrl(profile.imageUrl())
                    .blockedAt(friendConverter.toKstOffset(block.getCreatedAt()))
                    .build());
        }
        return toDto(items);
    }

    private BlockListResponseDTO toDto(List<BlockListResponseDTO.Item> items) {
        return BlockListResponseDTO.builder().count(items.size()).blocks(items).build();
    }
}
