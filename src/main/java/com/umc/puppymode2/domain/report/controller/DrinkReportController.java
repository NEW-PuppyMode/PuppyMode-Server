package com.umc.puppymode2.domain.report.controller;

import com.umc.puppymode2.domain.report.dto.DrinkReportResponseDTO;
import com.umc.puppymode2.domain.report.service.DrinkReportService;
import com.umc.puppymode2.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.umc.puppymode2.global.auth.context.UserContext;
import java.time.YearMonth;

@Validated
@RestController
@RequestMapping("/report")
@RequiredArgsConstructor
public class DrinkReportController {

    private final DrinkReportService drinkReportService;
    private final UserContext userContext;

    @GetMapping
    @Operation(summary = "목표 리포트 API", description = "선택 월(yyyy-MM) 기준으로 음주 리포트를 조회합니다. 예: 2025-08")
    public ResponseEntity<ApiResponse<DrinkReportResponseDTO>> getDrinkReport(
            @Parameter(description = "조회 연도 (예: 2025)", example = "2025")
            @RequestParam(name = "year") @Min(2000) @Max(2100) int year,

            @Parameter(description = "조회 월 (1~12)", example = "10")
            @RequestParam(name = "month") @Min(1) @Max(12) int month
    ) {
        Long userId = userContext.getCurrentUserId();

        YearMonth targetMonth = YearMonth.of(year, month);

        DrinkReportResponseDTO response = drinkReportService.drinkReport(userId, targetMonth);
        return ResponseEntity.ok(
                ApiResponse.onSuccess(response, "REPORT200","음주 리포트 조회 성공" )
        );
    }
}
