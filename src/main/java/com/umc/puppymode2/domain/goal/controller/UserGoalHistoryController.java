package com.umc.puppymode2.domain.goal.controller;

import com.umc.puppymode2.domain.goal.dto.MonthlyGoalRewardResponseDTO;
import com.umc.puppymode2.domain.goal.dto.GoalInfoResponseDTO;
import com.umc.puppymode2.domain.goal.dto.GoalPostRequestDTO;
import com.umc.puppymode2.domain.goal.dto.GoalPostResponseDTO;
import com.umc.puppymode2.domain.goal.service.MonthlyGoalRewardService;
import com.umc.puppymode2.domain.goal.service.UserGoalHistoryCommandService;
import com.umc.puppymode2.domain.goal.service.UserGoalHistoryQueryService;
import com.umc.puppymode2.global.apiPayload.ApiResponse;
import com.umc.puppymode2.global.auth.context.SecurityUserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

@Validated
@RestController
@RequestMapping("/goals")
@RequiredArgsConstructor
public class UserGoalHistoryController {

    private final UserGoalHistoryCommandService commandService;
    private final UserGoalHistoryQueryService queryService;
    private final SecurityUserContext securityUserContext;
    private final MonthlyGoalRewardService monthlyGoalRewardService;

    private Long getCurrentUserId() {
        return securityUserContext.getCurrentUserId();
    }

    // 1. 목표 등록 (신규/유지)
    @PostMapping
    @Operation(summary = "목표 등록 API", description = "목표 등록 API 입니다. 새로운 목표는 true, 기존 목표 유지는 false를 넣어주시면 됩니다.")
    public ResponseEntity<ApiResponse<GoalPostResponseDTO>> postGoal(
            @RequestBody @Valid GoalPostRequestDTO requestDto) {

        Long userId = getCurrentUserId();
        GoalPostResponseDTO response = commandService.postGoal(userId, requestDto);
        return ResponseEntity.ok(
                ApiResponse.onSuccess(response, "GOAL200", "목표 등록 성공")
        );
    }

    // 2. 최근 목표 조회
    @GetMapping
    @Operation(summary = "최근 목표 조회 API", description = "최근 목표 조회 API 입니다.")
    public ResponseEntity<ApiResponse<GoalInfoResponseDTO>> getLatestGoal() {
        Long userId = getCurrentUserId();
        GoalInfoResponseDTO response = queryService.getLatestGoal(userId);
        return ResponseEntity.ok(
                ApiResponse.onSuccess(response, "GOAL200", "최근 목표 조회 성공")
        );
    }

    // 3. 목표가 설정된 월 목록 조회
    @GetMapping("/months")
    @Operation(summary = "목표 설정 월 목록 조회 API",
            description = "사용자가 목표를 설정한 적 있는 월 목록을 \"yyyy-MM\" 형태로 오름차순 반환합니다. " +
                    "캘린더 모달에서 해당 월을 활성화하는 데 사용합니다. 목표 이력이 없으면 빈 배열입니다.")
    public ResponseEntity<ApiResponse<List<String>>> getGoalMonths() {
        Long userId = getCurrentUserId();
        List<String> response = queryService.getGoalMonths(userId);
        return ResponseEntity.ok(
                ApiResponse.onSuccess(response, "GOAL200", "목표 설정 월 목록 조회 성공")
        );
    }

    // 4. 이번 달 목표 설정 여부
    @GetMapping("/check-30days")
    @Operation(summary = "이번 달 목표 설정 여부 API", description = "이번 달 목표가 설정되어 있는지 확인합니다.")
    public ResponseEntity<ApiResponse<Boolean>> check30Days() {
        Long userId = getCurrentUserId();
        boolean isPassed = queryService.isMoreThan30DayPassed(userId);
        return ResponseEntity.ok(
                ApiResponse.onSuccess(isPassed, "GOAL200", "이번 달 목표 설정 여부 조회 성공")
        );
    }

    // 5. 월간 목표 보상 받기
    @PostMapping("/reward")
    @Operation(
            summary = "월간 목표 보상 받기 API",
            description = """
                  지난 달 목표 달성 보상(경험치 100)을 판정하고 지급합니다.
                  
                  - 다음 달 리포트 창에서 호출합니다.
                  - 기록 20일 이상 + 음주 횟수 ≤ 목표일 때 지급됩니다.
                  - 이미 받은 달은 저장된 before/after를 그대로 반환합니다.
                  - 여러 번 호출해도 한 번만 지급됩니다.
                  """
    )
    public ResponseEntity<ApiResponse<MonthlyGoalRewardResponseDTO>> grantMonthlyGoalReward(
            @Parameter(description = "보상 받을 연도", example = "2026")
            @RequestParam(name = "year") @Min(2000) @Max(2100) int year,

            @Parameter(description = "보상 받을 월 (1~12), 지난 달만 가능", example = "10")
            @RequestParam(name = "month") @Min(1) @Max(12) int month
    ) {
        Long userId = getCurrentUserId();
        MonthlyGoalRewardResponseDTO response =
                monthlyGoalRewardService.grantReward(userId, YearMonth.of(year, month));
        return ResponseEntity.ok(
                ApiResponse.onSuccess(response, "GOAL200", "월간 목표 보상 처리 성공")
        );
    }
}
