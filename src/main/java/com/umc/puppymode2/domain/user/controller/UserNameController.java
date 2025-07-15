package com.umc.puppymode2.domain.user.controller;

import com.umc.puppymode2.domain.user.dto.UserNameUpdateRequestDto;
import com.umc.puppymode2.domain.user.service.UserNameService;
import com.umc.puppymode2.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class UserNameController {

    private final UserNameService userNameService;

    @PostMapping("/my-name")
    @Operation(method = "POST", summary = "내 이름 알려주기 API", description = "사용자의 이름을 등록하는 API입니다.")
    public ApiResponse<Void> updateUsername(@RequestBody @Valid UserNameUpdateRequestDto requestDto) {
        userNameService.updateUsername(requestDto);
        return ApiResponse.onSuccess(null, "POST_MY_NAME_SUCCESS", "내 이름 알려주기 성공");
    }
}
