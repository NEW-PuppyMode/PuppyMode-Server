package com.umc.puppymode2.domain.friend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FriendRequestSendRequestDTO {

    // 상대의 친구 코드. 숫자 4~8자리 (앞뒤 공백은 아래 setter에서 제거한 뒤 검증한다)
    @NotNull(message = "친구 코드를 확인해주세요")
    @Pattern(regexp = "^\\d{4,8}$", message = "친구 코드를 확인해주세요")
    private String friendCode;

    // Jackson이 역직렬화 때 이 setter를 사용하므로, 검증(@Pattern) 전에 공백이 제거된다.
    public void setFriendCode(String friendCode) {
        this.friendCode = friendCode == null ? null : friendCode.trim();
    }
}
