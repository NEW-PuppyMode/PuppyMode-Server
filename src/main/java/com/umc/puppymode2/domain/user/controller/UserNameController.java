package com.umc.puppymode2.domain.user.controller;

import com.umc.puppymode2.domain.user.dto.UserNameUpdateRequestDto;
import com.umc.puppymode2.domain.user.service.UserNameService;
import com.umc.puppymode2.global.apiPayload.ApiResponse;
import com.umc.puppymode2.global.apiPayload.code.status.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class UserNameController {

    private final UserNameService userNameService;

    @PatchMapping("/user-name")
    @Operation(
            method = "PATCH",
            summary = "유저 이름 수정 API",
            description = "유저의 이름을 수정하는 API입니다. (최초 설정 및 이후 이름 변경 시 공용으로 사용됩니다.) 최초 수정 시에만 경험치가 지급됩니다."
    )
    public ApiResponse<Void> updateUsername(@Valid @RequestBody UserNameUpdateRequestDto requestDto) {
        userNameService.updateUsername(requestDto);
        return ApiResponse.onSuccess(
                null,
                SuccessStatus.USER_NAME_UPDATE_SUCCESS.getCode(),
                SuccessStatus.USER_NAME_UPDATE_SUCCESS.getMessage()
        );
    }
}