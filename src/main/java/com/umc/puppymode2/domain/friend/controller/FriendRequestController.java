package com.umc.puppymode2.domain.friend.controller;

import com.umc.puppymode2.domain.friend.dto.FriendRequestAcceptResponseDTO;
import com.umc.puppymode2.domain.friend.dto.FriendRequestSendRequestDTO;
import com.umc.puppymode2.domain.friend.dto.FriendRequestSendResponseDTO;
import com.umc.puppymode2.domain.friend.dto.ReceivedFriendRequestListResponseDTO;
import com.umc.puppymode2.domain.friend.service.FriendCommandService;
import com.umc.puppymode2.domain.friend.service.FriendQueryService;
import com.umc.puppymode2.global.apiPayload.ApiResponse;
import com.umc.puppymode2.global.auth.context.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/friend-requests")
@RequiredArgsConstructor
public class FriendRequestController {

    private final FriendCommandService commandService;
    private final FriendQueryService queryService;
    private final UserContext userContext;

    // 1. 친구 요청 보내기
    @PostMapping
    @Operation(summary = "친구 요청 보내기 API",
            description = "상대의 친구 코드(숫자 4~8자리)로 친구 요청을 보냅니다. 상대가 이미 나에게 요청한 상태라면 자동 수락되어 " +
                    "즉시 친구가 되며 autoAccepted=true, status=ACCEPTED로 응답합니다. " +
                    "코드 입력 실패(없는 코드/내 코드)가 한도를 넘으면 429(FRIEND4291)를 반환합니다.")
    public ResponseEntity<ApiResponse<FriendRequestSendResponseDTO>> sendFriendRequest(
            @RequestBody @Valid FriendRequestSendRequestDTO requestDto) {
        Long userId = userContext.getCurrentUserId();
        FriendRequestSendResponseDTO response = commandService.sendFriendRequest(userId, requestDto.getFriendCode());
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.onSuccess(response, "FRIEND201", "친구 요청 성공")
        );
    }

    // 2. 받은 친구 요청 목록
    @GetMapping("/received")
    @Operation(summary = "받은 친구 요청 목록 API", description = "내가 받은 PENDING 요청만 최신순으로 반환합니다.")
    public ResponseEntity<ApiResponse<ReceivedFriendRequestListResponseDTO>> getReceivedRequests() {
        Long userId = userContext.getCurrentUserId();
        ReceivedFriendRequestListResponseDTO response = queryService.getReceivedRequests(userId);
        return ResponseEntity.ok(
                ApiResponse.onSuccess(response, "FRIEND200", "받은 친구 요청 목록 조회 성공")
        );
    }

    // 3. 친구 요청 수락
    @PostMapping("/{requestId}/accept")
    @Operation(summary = "친구 요청 수락 API", description = "요청을 수락하고 친구 관계를 생성합니다. 응답의 friendUserId는 새 친구의 userId입니다.")
    public ResponseEntity<ApiResponse<FriendRequestAcceptResponseDTO>> acceptFriendRequest(
            @Parameter(description = "친구 요청 ID", example = "101")
            @PathVariable("requestId") Long requestId) {
        Long userId = userContext.getCurrentUserId();
        FriendRequestAcceptResponseDTO response = commandService.acceptFriendRequest(userId, requestId);
        return ResponseEntity.ok(
                ApiResponse.onSuccess(response, "FRIEND200", "친구 요청 수락 성공")
        );
    }

    // 4. 친구 요청 거절
    @PostMapping("/{requestId}/reject")
    @Operation(summary = "친구 요청 거절 API", description = "요청을 거절합니다. 요청자에게는 알리지 않습니다.")
    public ResponseEntity<ApiResponse<Void>> rejectFriendRequest(
            @Parameter(description = "친구 요청 ID", example = "101")
            @PathVariable("requestId") Long requestId) {
        Long userId = userContext.getCurrentUserId();
        commandService.rejectFriendRequest(userId, requestId);
        return ResponseEntity.ok(
                ApiResponse.onSuccess("FRIEND200", "친구 요청 거절 성공")
        );
    }
}
