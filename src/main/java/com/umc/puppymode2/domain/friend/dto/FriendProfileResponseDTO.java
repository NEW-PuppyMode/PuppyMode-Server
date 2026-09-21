package com.umc.puppymode2.domain.friend.dto;

import com.umc.puppymode2.domain.puppy.entity.PuppyType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FriendProfileResponseDTO {
    private Long userId;
    private String username;
    private String puppyName;
    private PuppyType puppyType; // BICHON / SHIBA / CORGI / POODLE
    private Integer level;
    private String profileImageUrl; // 강아지 이미지 URL
}
