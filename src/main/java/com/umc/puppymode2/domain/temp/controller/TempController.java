package com.umc.puppymode2.domain.temp.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.umc.puppymode2.domain.temp.converter.TempConverter;
import com.umc.puppymode2.domain.temp.dto.TempRequestDTO;
import com.umc.puppymode2.domain.temp.dto.TempResponseDTO;
import com.umc.puppymode2.domain.temp.entity.Temp;
import com.umc.puppymode2.domain.temp.entity.enums.TempStatus;
import com.umc.puppymode2.domain.temp.service.TempCommandService;
import com.umc.puppymode2.domain.temp.service.TempQueryService;
import com.umc.puppymode2.global.apiPayload.ApiResponse;

import jakarta.validation.Valid;
import java.util.List;

@Tag(name = "temp-controller", description = "임시 데이터 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/temp")
public class TempController {

    private final TempCommandService tempCommandService;
    private final TempQueryService tempQueryService;

    @Operation(summary = "임시 데이터 생성", description = "새로운 임시 데이터를 생성합니다.")
    @PostMapping("/")
    public ApiResponse<TempResponseDTO.CreateTempResultDTO> createTemp(
            @RequestBody @Valid TempRequestDTO.CreateTempDTO request) {
        Long tempId = tempCommandService.createTemp(request);
        return ApiResponse.onSuccess(TempConverter.toCreateTempResultDTO(tempId));
    }

    @Operation(summary = "임시 데이터 단건 조회", description = "ID로 임시 데이터를 조회합니다.")
    @GetMapping("/{tempId}")
    public ApiResponse<TempResponseDTO.TempDTO> getTemp(
            @Parameter(description = "임시 데이터 ID") @PathVariable Long tempId) {
        Temp temp = tempQueryService.findTempById(tempId);
        return ApiResponse.onSuccess(TempConverter.toTempDTO(temp));
    }

    @Operation(summary = "임시 데이터 전체 조회", description = "모든 임시 데이터를 조회합니다.")
    @GetMapping("/")
    public ApiResponse<TempResponseDTO.TempListDTO> getAllTemps() {
        List<Temp> temps = tempQueryService.findAllTemps();
        return ApiResponse.onSuccess(TempConverter.toTempListDTO(temps));
    }

    @Operation(summary = "상태별 임시 데이터 조회", description = "상태로 임시 데이터를 조회합니다.")
    @GetMapping("/status/{status}")
    public ApiResponse<TempResponseDTO.TempListDTO> getTempsByStatus(
            @Parameter(description = "임시 데이터 상태") @PathVariable TempStatus status) {
        List<Temp> temps = tempQueryService.findTempsByStatus(status);
        return ApiResponse.onSuccess(TempConverter.toTempListDTO(temps));
    }

    @Operation(summary = "임시 데이터 수정", description = "임시 데이터를 수정합니다.")
    @PatchMapping("/{tempId}")
    public ApiResponse<String> updateTemp(
            @Parameter(description = "임시 데이터 ID") @PathVariable Long tempId,
            @RequestBody @Valid TempRequestDTO.UpdateTempDTO request) {
        tempCommandService.updateTemp(tempId, request);
        return ApiResponse.onSuccess("임시 데이터가 성공적으로 수정되었습니다.");
    }

    @Operation(summary = "임시 데이터 삭제", description = "임시 데이터를 삭제합니다.")
    @DeleteMapping("/{tempId}")
    public ApiResponse<String> deleteTemp(
            @Parameter(description = "임시 데이터 ID") @PathVariable Long tempId) {
        tempCommandService.deleteTemp(tempId);
        return ApiResponse.onSuccess("임시 데이터가 성공적으로 삭제되었습니다.");
    }
}
