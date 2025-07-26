package com.umc.puppymode2.domain.report.controller;

import com.umc.puppymode2.domain.report.dto.DrinkReportResponseDTO;
import com.umc.puppymode2.domain.report.service.DrinkReportService;
import com.umc.puppymode2.global.apiPayload.ApiResponse;
import com.umc.puppymode2.global.auth.context.SecurityUserContext;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.umc.puppymode2.global.auth.context.UserContext;

@RestController
@RequestMapping("/report")
@RequiredArgsConstructor
public class DrinkReportController {

    private final DrinkReportService drinkReportService;
    private final UserContext userContext;

    @GetMapping
    @Operation(summary = "목표 리포트 API", description = "목표 리포트 조회 API 입니다.")
    public ResponseEntity<ApiResponse<DrinkReportResponseDTO>> getDrinkReport() {
        Long userId = userContext.getCurrentUserId();
        DrinkReportResponseDTO response = drinkReportService.drinkReport(userId);
        return ResponseEntity.ok(
                ApiResponse.onSuccess(response, "REPORT200","음주 리포트 조회 성공" )
        );
    }
}
