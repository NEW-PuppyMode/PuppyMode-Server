package com.umc.puppymode2.domain.complaint.controller;

import com.umc.puppymode2.domain.complaint.dto.ComplaintCreateRequestDTO;
import com.umc.puppymode2.domain.complaint.dto.ComplaintCreateResponseDTO;
import com.umc.puppymode2.domain.complaint.service.ComplaintCommandService;
import com.umc.puppymode2.global.apiPayload.ApiResponse;
import com.umc.puppymode2.global.auth.context.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/complaints")
@RequiredArgsConstructor
public class ComplaintController {

    private final ComplaintCommandService commandService;
    private final UserContext userContext;

    @PostMapping
    @Operation(summary = "신고하기 API",
            description = "사용자를 신고합니다. 사유가 OTHER(기타)일 때만 detail(직접 입력)이 필수입니다. " +
                    "같은 대상을 같은 사유로 다시 신고하면 409(COMPLAINT4091)이고, 다른 사유로는 신고할 수 있습니다. " +
                    "신고해도 자동으로 차단되지 않습니다.")
    public ResponseEntity<ApiResponse<ComplaintCreateResponseDTO>> createComplaint(
            @RequestBody @Valid ComplaintCreateRequestDTO requestDto) {
        Long userId = userContext.getCurrentUserId();
        ComplaintCreateResponseDTO response = commandService.createComplaint(userId, requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.onSuccess(response, "COMPLAINT201", "신고 접수 성공")
        );
    }
}
