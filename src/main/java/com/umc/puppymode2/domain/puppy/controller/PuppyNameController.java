package com.umc.puppymode2.domain.puppy.controller;

import com.umc.puppymode2.domain.puppy.dto.PuppyNameRequestDto;
import com.umc.puppymode2.domain.puppy.service.PuppyNameService;
import com.umc.puppymode2.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class PuppyNameController {

    private final PuppyNameService puppyNameService;

    @PostMapping("/puppy-name")
    @Operation(method = "POST", summary = "강아지 이름 짓기 API", description = "강아지의 이름을 등록하는 API입니다.")
    public ApiResponse<Void> updatePuppyName(@Valid @RequestBody PuppyNameRequestDto requestDto) {
        puppyNameService.updatePuppyName(requestDto);
        return ApiResponse.onSuccess(null, "POST_PUPPY_NAME_SUCCESS", "강아지 이름 지어주기 성공");
    }
}
