package com.umc.puppymode2.domain.drinkhistory.controller;

import com.umc.puppymode2.domain.drinkhistory.dto.DrinkHistoryRequestDTO;
import com.umc.puppymode2.domain.drinkhistory.dto.DrinkHistoryResponseDTO;
import com.umc.puppymode2.domain.drinkhistory.dto.DrinkHistoryStatusDTO;
import com.umc.puppymode2.domain.drinkhistory.service.DrinkHistoryCommandService;
import com.umc.puppymode2.domain.drinkhistory.service.DrinkHistoryQueryService;
import com.umc.puppymode2.global.apiPayload.ApiResponse;
import com.umc.puppymode2.global.apiPayload.code.status.SuccessStatus;
import com.umc.puppymode2.global.auth.context.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "drink-history-controller", description = "음주 기록 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/drink-history")
public class DrinkHistoryController {
    private final UserContext userContext;
    private final DrinkHistoryCommandService drinkHistoryCommandService;
    private final DrinkHistoryQueryService drinkHistoryQueryService;

    @Operation(summary = "음주 기록 생성 API", description = "새로운 음주 기록을 생성합니다.")
    @PostMapping
    public ApiResponse<DrinkHistoryResponseDTO> createDrinkHistory(
            @RequestBody @Valid DrinkHistoryRequestDTO request) {
        Long userId = userContext.getCurrentUserId();
        DrinkHistoryResponseDTO drinkHistoryResponseDTO = drinkHistoryCommandService.recordDrink(userId, request);
        return ApiResponse.onSuccess(drinkHistoryResponseDTO, SuccessStatus.DRINK_HISTORY_RECORD_SUCCESS.getCode(), SuccessStatus.DRINK_HISTORY_RECORD_SUCCESS.getMessage());
    }

    @Operation(summary = "음주 기록 상태 조회 API", description = "어제와 오늘의 음주 기록 상태를 조회합니다.")
    @GetMapping("/status")
    public ApiResponse<DrinkHistoryStatusDTO> getRecordStatus() {
        Long userId = userContext.getCurrentUserId();
        DrinkHistoryStatusDTO drinkHistoryStatusDTO = drinkHistoryQueryService.getDrinkRecordStatus(userId);
        return ApiResponse.onSuccess(drinkHistoryStatusDTO, SuccessStatus.DRINK_HISTORY_STATUS_SUCCESS.getCode(), SuccessStatus.DRINK_HISTORY_STATUS_SUCCESS.getMessage());
    }
}
