package com.umc.puppymode2.domain.social.controller;

import com.umc.puppymode2.domain.social.dto.SocialSummaryResponseDTO;
import com.umc.puppymode2.domain.social.service.SocialSummaryService;
import com.umc.puppymode2.global.apiPayload.ApiResponse;
import com.umc.puppymode2.global.auth.context.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/social")
@RequiredArgsConstructor
public class SocialController {

    private final SocialSummaryService socialSummaryService;
    private final UserContext userContext;

    @GetMapping("/summary")
    @Operation(summary = "소셜 뱃지 API",
            description = "홈 소셜 버튼의 빨간 원 표시 여부를 반환합니다. 받은 친구 요청이 있거나 안 읽은 응원이 있으면 hasBadge=true입니다.")
    public ResponseEntity<ApiResponse<SocialSummaryResponseDTO>> getSocialSummary() {
        Long userId = userContext.getCurrentUserId();
        SocialSummaryResponseDTO response = socialSummaryService.getSummary(userId);
        return ResponseEntity.ok(
                ApiResponse.onSuccess(response, "SOCIAL200", "소셜 뱃지 조회 성공")
        );
    }
}
