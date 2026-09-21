package com.umc.puppymode2.domain.cheer.controller;

import com.umc.puppymode2.domain.cheer.dto.CheerSendRequestDTO;
import com.umc.puppymode2.domain.cheer.dto.CheerSendResponseDTO;
import com.umc.puppymode2.domain.cheer.service.CheerCommandService;
import com.umc.puppymode2.global.apiPayload.ApiResponse;
import com.umc.puppymode2.global.auth.context.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 응원 보내기. 경로가 /friends/{userId}/cheers 이라 친구 컨트롤러와 같은 접두사를 쓰지만,
 * friend 도메인 코드를 건드리지 않도록 응원 도메인의 별도 컨트롤러로 둔다.
 */
@RestController
@RequestMapping("/friends")
@RequiredArgsConstructor
public class FriendCheerController {

    private final CheerCommandService commandService;
    private final UserContext userContext;

    @PostMapping("/{userId}/cheers")
    @Operation(summary = "응원 보내기 API",
            description = "친구에게 응원을 보냅니다. targetDate는 친구 목록의 cheer.targetDate 값을 그대로 전달하며, " +
                    "어제 또는 오늘이면서 친구가 그 날 실제로 마신 날짜여야 합니다. " +
                    "같은 (보낸 사람, 받는 사람, 날짜)에는 한 번만 보낼 수 있습니다.")
    public ResponseEntity<ApiResponse<CheerSendResponseDTO>> sendCheer(
            @Parameter(description = "응원을 받을 친구의 userId", example = "12")
            @PathVariable("userId") Long friendUserId,
            @RequestBody @Valid CheerSendRequestDTO requestDto) {
        Long myUserId = userContext.getCurrentUserId();
        CheerSendResponseDTO response = commandService.sendCheer(myUserId, friendUserId, requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.onSuccess(response, "CHEER201", "응원 보내기 성공")
        );
    }
}
