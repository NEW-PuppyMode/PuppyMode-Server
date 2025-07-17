package com.umc.puppymode2.domain.user.controller;

import com.umc.puppymode2.domain.user.service.UserCommandService;
import com.umc.puppymode2.global.apiPayload.ApiResponse;
import com.umc.puppymode2.global.auth.context.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserCommandService userCommandService;
    private final UserContext userContext;

    @Operation(
            summary = "카카오 소셜 회원 탈퇴",
            description = "카카오 소셜 로그인을 한 사용자가 자신의 계정을 탈퇴(연결끊기)합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "탈퇴하기 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패/토큰 만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 유저"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping("/withdraw")
    public ResponseEntity<ApiResponse<Void>> withdraw() {
        Long userId = userContext.getCurrentUserId();
        userCommandService.withdrawUser(userId);
        return ResponseEntity.ok(ApiResponse.onSuccess(null, "POST_WITHDRAW", "탈퇴하기 성공"));
    }

}
