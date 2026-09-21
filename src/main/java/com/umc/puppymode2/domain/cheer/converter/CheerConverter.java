package com.umc.puppymode2.domain.cheer.converter;

import com.umc.puppymode2.domain.cheer.dto.*;
import com.umc.puppymode2.domain.cheer.entity.Cheer;
import com.umc.puppymode2.domain.cheer.entity.CheerTemplate;
import com.umc.puppymode2.domain.cheer.entity.enums.CheerCategory;
import com.umc.puppymode2.domain.friend.cache.PuppyProfileCache.PuppyProfile;
import com.umc.puppymode2.domain.friend.converter.FriendConverter;
import com.umc.puppymode2.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CheerConverter {

    // LocalDateTime(JVM 기본 타임존 기준) → KST(+09:00) 변환은 friend 도메인과 같은 기준을 쓰기 위해 그쪽 변환기를 재사용한다.
    private final FriendConverter friendConverter;

    /**
     * 활성 문구를 분류별로 묶는다. 분류는 항상 장난 → 위로 → 응원 순서(enum 선언 순서)로 모두 내려주고,
     * 문구가 하나도 없는 분류는 빈 배열로 둔다. 분류 안의 순서는 입력 리스트의 순서(display_order)를 유지한다.
     */
    public CheerTemplateListResponseDTO toTemplateListDto(List<CheerTemplate> templates) {
        List<CheerTemplateListResponseDTO.Category> categories = new ArrayList<>();

        for (CheerCategory category : CheerCategory.values()) {
            List<CheerTemplateListResponseDTO.Template> items = templates.stream()
                    .filter(template -> template.getCategory() == category)
                    .map(template -> CheerTemplateListResponseDTO.Template.builder()
                            .id(template.getCheerTemplateId())
                            .message(template.getMessage())
                            .build())
                    .toList();

            categories.add(CheerTemplateListResponseDTO.Category.builder()
                    .category(category)
                    .label(category.getLabel())
                    .templates(items)
                    .build());
        }
        return CheerTemplateListResponseDTO.builder().categories(categories).build();
    }

    public CheerSendResponseDTO toSendDto(Cheer cheer) {
        return CheerSendResponseDTO.builder()
                .cheerId(cheer.getCheerId())
                .sentAt(friendConverter.toKstOffset(cheer.getCreatedAt()))
                .build();
    }

    public ReceivedCheerListResponseDTO.Item toReceivedItem(
            Cheer cheer, User sender, PuppyProfile senderProfile, CheerExpiresIn expiresIn) {
        return ReceivedCheerListResponseDTO.Item.builder()
                .cheerId(cheer.getCheerId())
                .sender(ReceivedCheerListResponseDTO.Sender.builder()
                        .userId(sender.getUserId())
                        .username(sender.getUsername())
                        .profileImageUrl(senderProfile.imageUrl())
                        .build())
                .category(cheer.getCategory())
                .message(cheer.getMessageSnapshot())
                .receivedAt(friendConverter.toKstOffset(cheer.getCreatedAt()))
                .expiresAt(friendConverter.toKstOffset(cheer.getExpiresAt()))
                .expiresIn(expiresIn)
                .isRead(cheer.isRead())
                .build();
    }

    public ReceivedCheerListResponseDTO toReceivedListDto(List<ReceivedCheerListResponseDTO.Item> items) {
        return ReceivedCheerListResponseDTO.builder()
                .count(items.size())
                .cheers(items)
                .build();
    }
}
