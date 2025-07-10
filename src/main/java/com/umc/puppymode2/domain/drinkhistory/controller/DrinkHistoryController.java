package com.umc.puppymode2.domain.drinkhistory.controller;

import com.umc.puppymode2.domain.drinkhistory.dto.DrinkHistoryRequestDTO;
import com.umc.puppymode2.domain.drinkhistory.dto.DrinkHistoryResponseDTO;
import com.umc.puppymode2.domain.drinkhistory.service.DrinkHistoryCommandService;
import com.umc.puppymode2.domain.drinkhistory.service.DrinkHistoryQueryService;
import com.umc.puppymode2.domain.temp.converter.TempConverter;
import com.umc.puppymode2.domain.temp.dto.TempRequestDTO;
import com.umc.puppymode2.domain.temp.dto.TempResponseDTO;
import com.umc.puppymode2.domain.temp.service.TempCommandService;
import com.umc.puppymode2.domain.temp.service.TempQueryService;
import com.umc.puppymode2.global.apiPayload.ApiResponse;
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
    private final DrinkHistoryCommandService drinkHistoryCommandService;
    private final DrinkHistoryQueryService drinkHistoryQueryService;
    @Operation(summary = "음주 기록 생성", description = "새로운 음주 기록을 생성합니다.")
    @PostMapping
    public ApiResponse<Long> createDrinkHistory(
            @RequestBody @Valid DrinkHistoryRequestDTO.CreateDrinkHistory request) {
        Long userId = 1L;
        Long drinkHistoryId = drinkHistoryCommandService.recordDrink(userId, request);
        return ApiResponse.onSuccess(drinkHistoryId);
    }

    @Operation(summary = "음주 기록 상태 조회", description = "음주 기록 상태를 조회합니다.")
    @GetMapping("/status")
    public ApiResponse<DrinkHistoryResponseDTO.DrinkStatus> getRecordStatus() {
        Long userId = 1L;
        DrinkHistoryResponseDTO.DrinkStatus drinkStatus = drinkHistoryQueryService.getDrinkRecordStatus(userId);
        return ApiResponse.onSuccess(drinkStatus);
    }
}
