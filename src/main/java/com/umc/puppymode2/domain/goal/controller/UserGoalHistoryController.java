package com.umc.puppymode2.domain.goal.controller;

import com.umc.puppymode2.domain.goal.dto.GoalInfoResponseDTO;
import com.umc.puppymode2.domain.goal.dto.GoalPostRequestDTO;
import com.umc.puppymode2.domain.goal.dto.GoalPostResponseDTO;
import com.umc.puppymode2.domain.goal.service.UserGoalHistoryCommandService;
import com.umc.puppymode2.domain.goal.service.UserGoalHistoryQueryService;
import com.umc.puppymode2.domain.user.service.KakaoAuthService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/goals")
@RequiredArgsConstructor
public class UserGoalHistoryController {

    private final UserGoalHistoryCommandService commandService;
    private final UserGoalHistoryQueryService queryService;

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        System.out.println("authentication.getName() = " + authentication.getName());
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("인증 정보가 없습니다.");
        }

        return Long.parseLong(authentication.getName());
    }

    // 1. 목표 등록 (신규/유지)
    @PostMapping
    @Operation(summary = "목표 등록 API", description = "목표 등록 API 입니다. 새로운 목표는 true, 기존 목표 유지는 false를 넣어주시면 됩니다.")
    public ResponseEntity<GoalPostResponseDTO> postGoal(
            @RequestBody @Valid GoalPostRequestDTO requestDto) {

        Long userId = getCurrentUserId();
        GoalPostResponseDTO response = commandService.postGoal(userId, requestDto);
        return ResponseEntity.ok(response);
    }

    // 2. 최근 목표 조회
    @GetMapping
    @Operation(summary = "최근 목표 조회 API", description = "최근 목표 조회 API 입니다.")
    public ResponseEntity<GoalInfoResponseDTO> getLatestGoal() {
        Long userId = getCurrentUserId();
        GoalInfoResponseDTO response = queryService.getLatestGoal(userId);
        return ResponseEntity.ok(response);
    }

    // 3. 목표 설정 후 30일 경과 여부
    @GetMapping("/check-30days")
    @Operation(summary = "목표 설정 후 30일 경과 여부 API", description = "목표 설정 후 30일 경과 여부 API 입니다.")
    public ResponseEntity<Boolean> check30Days() {
        Long userId = getCurrentUserId();
        boolean isPassed = queryService.isMoreThan30DayPassed(userId);
        return ResponseEntity.ok(isPassed);
    }
}
