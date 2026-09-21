package com.umc.puppymode2.domain.cheer.controller;

import com.umc.puppymode2.domain.cheer.dto.ReceivedCheerListResponseDTO;
import com.umc.puppymode2.domain.cheer.service.CheerCommandService;
import com.umc.puppymode2.domain.cheer.service.CheerQueryService;
import com.umc.puppymode2.global.apiPayload.ApiResponse;
import com.umc.puppymode2.global.auth.context.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cheers")
@RequiredArgsConstructor
public class CheerController {

    private final CheerQueryService queryService;
    private final CheerCommandService commandService;
    private final UserContext userContext;

    @GetMapping("/received")
    @Operation(summary = "받은 응원 목록 API",
            description = "만료 전 응원을 받은 순서 최신순으로 반환합니다. 현재 친구인 발신자의 응원만 포함하며 페이지네이션은 없습니다. " +
                    "expiresIn은 TODAY(오늘 밤 12시에 사라짐) / TOMORROW(내일 밤 12시에 사라짐)입니다.")
    public ResponseEntity<ApiResponse<ReceivedCheerListResponseDTO>> getReceivedCheers() {
        Long userId = userContext.getCurrentUserId();
        ReceivedCheerListResponseDTO response = queryService.getReceivedCheers(userId);
        return ResponseEntity.ok(
                ApiResponse.onSuccess(response, "CHEER200", "받은 응원 목록 조회 성공")
        );
    }

    @PostMapping("/received/read")
    @Operation(summary = "받은 응원 읽음 처리 API",
            description = "내가 받은 안 읽은 응원을 모두 읽음 처리합니다. 「받은 응원」 탭 진입 시 호출합니다. 이미 모두 읽은 상태여도 성공합니다.")
    public ResponseEntity<ApiResponse<Void>> markReceivedCheersAsRead() {
        Long userId = userContext.getCurrentUserId();
        commandService.markAllReceivedAsRead(userId);
        return ResponseEntity.ok(
                ApiResponse.onSuccess("CHEER200", "받은 응원 읽음 처리 성공")
        );
    }
}
