package com.umc.puppymode2.domain.friend.dto;

import com.umc.puppymode2.domain.puppy.entity.PuppyType;
import io.swagger.v3.oas.annotations.media.Schema;
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
    @Schema(description = "BICHON / SHIBA / CORGI / POODLE (강아지가 없으면 null)", example = "SHIBA", nullable = true)
    private PuppyType puppyType; // BICHON / SHIBA / CORGI / POODLE
    private Integer level;
}
