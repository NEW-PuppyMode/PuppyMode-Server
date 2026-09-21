package com.umc.puppymode2.domain.friend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FriendCodeResponseDTO {
    private String code; // 내 친구 코드 (현재 4자리 숫자)
}
