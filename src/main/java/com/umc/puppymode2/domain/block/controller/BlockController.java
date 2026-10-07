package com.umc.puppymode2.domain.block.controller;

import com.umc.puppymode2.domain.block.dto.BlockCreateRequestDTO;
import com.umc.puppymode2.domain.block.dto.BlockCreateResponseDTO;
import com.umc.puppymode2.domain.block.dto.BlockListResponseDTO;
import com.umc.puppymode2.domain.block.service.BlockCommandService;
import com.umc.puppymode2.domain.block.service.BlockQueryService;
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
@RequestMapping("/blocks")
@RequiredArgsConstructor
public class BlockController {

    private final BlockCommandService commandService;
    private final BlockQueryService queryService;
    private final UserContext userContext;

    @PostMapping
    @Operation(summary = "차단하기 API",
            description = "친구를 차단합니다. 차단하면 두 사람의 친구 관계, 둘 사이의 응원, 대기 중 친구 요청이 모두 삭제되며 " +
                    "상대에게는 알리지 않습니다. 친구가 아니면 403(BLOCK4031), 이미 차단했으면 409(BLOCK4091)입니다.")
    public ResponseEntity<ApiResponse<BlockCreateResponseDTO>> block(
            @RequestBody @Valid BlockCreateRequestDTO requestDto) {
        Long userId = userContext.getCurrentUserId();
        BlockCreateResponseDTO response = commandService.block(userId, requestDto.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.onSuccess(response, "BLOCK201", "차단 성공")
        );
    }

    @GetMapping
    @Operation(summary = "차단 목록 조회 API", description = "내가 차단한 사용자를 최근 차단순으로 반환합니다.")
    public ResponseEntity<ApiResponse<BlockListResponseDTO>> getBlocks() {
        Long userId = userContext.getCurrentUserId();
        BlockListResponseDTO response = queryService.getBlocks(userId);
        return ResponseEntity.ok(
                ApiResponse.onSuccess(response, "BLOCK200", "차단 목록 조회 성공")
        );
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "차단 해제 API",
            description = "차단을 해제합니다. 친구 관계는 복구되지 않으며, 다시 친구가 되려면 친구 요청을 새로 보내야 합니다.")
    public ResponseEntity<ApiResponse<Void>> unblock(
            @Parameter(description = "차단을 해제할 사용자의 userId", example = "12")
            @PathVariable("userId") Long blockedUserId) {
        Long userId = userContext.getCurrentUserId();
        commandService.unblock(userId, blockedUserId);
        return ResponseEntity.ok(
                ApiResponse.onSuccess("BLOCK200", "차단 해제 성공")
        );
    }
}
