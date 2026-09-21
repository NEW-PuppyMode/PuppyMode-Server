package com.umc.puppymode2.domain.friend.controller;

import com.umc.puppymode2.domain.friend.dto.FriendCodeResponseDTO;
import com.umc.puppymode2.domain.friend.dto.FriendListResponseDTO;
import com.umc.puppymode2.domain.friend.dto.FriendProfileResponseDTO;
import com.umc.puppymode2.domain.friend.service.FriendCodeService;
import com.umc.puppymode2.domain.friend.service.FriendCommandService;
import com.umc.puppymode2.domain.friend.service.FriendQueryService;
import com.umc.puppymode2.global.apiPayload.ApiResponse;
import com.umc.puppymode2.global.auth.context.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendCodeService friendCodeService;
    private final FriendCommandService commandService;
    private final FriendQueryService queryService;
    private final UserContext userContext;

    // 1. 내 친구 코드 조회
    @GetMapping("/code")
    @Operation(summary = "내 친구 코드 조회 API",
            description = "내 친구 코드를 조회합니다. 아직 코드가 없으면 이 시점에 발급하며, 이후에는 같은 코드를 반환합니다.")
    public ResponseEntity<ApiResponse<FriendCodeResponseDTO>> getMyFriendCode() {
        Long userId = userContext.getCurrentUserId();
        FriendCodeResponseDTO response = friendCodeService.getOrIssueMyCode(userId);
        return ResponseEntity.ok(
                ApiResponse.onSuccess(response, "FRIEND200", "내 친구코드 조회 성공")
        );
    }

    // 2. 내 친구 목록
    @GetMapping
    @Operation(summary = "내 친구 목록 API",
            description = "내 친구를 가나다순(username)으로 반환합니다. 친구의 음주 문구 상태(drinkStatus)와 응원 버튼 상태(cheer)를 함께 내려줍니다.")
    public ResponseEntity<ApiResponse<FriendListResponseDTO>> getFriends() {
        Long userId = userContext.getCurrentUserId();
        FriendListResponseDTO response = queryService.getFriends(userId);
        return ResponseEntity.ok(
                ApiResponse.onSuccess(response, "FRIEND200", "친구 목록 조회 성공")
        );
    }

    // 3. 친구 프로필 조회
    @GetMapping("/{userId}/profile")
    @Operation(summary = "친구 프로필 조회 API", description = "친구 프로필 팝업(레벨·이름·강아지 이미지)용입니다. 친구만 조회할 수 있습니다.")
    public ResponseEntity<ApiResponse<FriendProfileResponseDTO>> getFriendProfile(
            @Parameter(description = "조회할 친구의 userId", example = "12")
            @PathVariable("userId") Long friendUserId) {
        Long myUserId = userContext.getCurrentUserId();
        FriendProfileResponseDTO response = queryService.getFriendProfile(myUserId, friendUserId);
        return ResponseEntity.ok(
                ApiResponse.onSuccess(response, "FRIEND200", "친구 프로필 조회 성공")
        );
    }

    // 4. 친구 삭제
    @DeleteMapping("/{userId}")
    @Operation(summary = "친구 삭제 API", description = "친구 관계를 삭제합니다. 차단이 아니므로 이후 다시 친구 요청을 할 수 있습니다.")
    public ResponseEntity<ApiResponse<Void>> deleteFriend(
            @Parameter(description = "삭제할 친구의 userId", example = "12")
            @PathVariable("userId") Long friendUserId) {
        Long myUserId = userContext.getCurrentUserId();
        commandService.deleteFriend(myUserId, friendUserId);
        return ResponseEntity.ok(
                ApiResponse.onSuccess("FRIEND200", "친구 삭제 성공")
        );
    }
}
