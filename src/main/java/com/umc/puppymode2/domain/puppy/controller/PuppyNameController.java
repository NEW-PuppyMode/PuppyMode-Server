package com.umc.puppymode2.domain.puppy.controller;

import com.umc.puppymode2.domain.puppy.dto.PuppyNameRequestDto;
import com.umc.puppymode2.domain.puppy.service.PuppyNameService;
import com.umc.puppymode2.global.apiPayload.ApiResponse;
import com.umc.puppymode2.global.apiPayload.code.status.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class PuppyNameController {

    private final PuppyNameService puppyNameService;

    @PatchMapping("/puppy-name")
    @Operation(
            method = "PATCH",
            summary = "강아지 이름 수정 API",
            description = "강아지의 이름을 수정하는 API입니다. (최초 설정 및 이후 이름 변경 시 공용으로 사용됩니다.) 최초 수정 시에만 경험치가 지급됩니다."
    )
    public ApiResponse<Void> updatePuppyName(@Valid @RequestBody PuppyNameRequestDto requestDto) {
        puppyNameService.updatePuppyName(requestDto);
        return ApiResponse.onSuccess(
                null,
                SuccessStatus.PUPPY_NAME_UPDATE_SUCCESS.getCode(),
                SuccessStatus.PUPPY_NAME_UPDATE_SUCCESS.getMessage()
        );
    }
}