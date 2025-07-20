package com.umc.puppymode2.domain.puppy.controller;

import com.umc.puppymode2.domain.puppy.dto.MainResponseDto;
import com.umc.puppymode2.domain.puppy.service.MainService;
import com.umc.puppymode2.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/main")
@RequiredArgsConstructor
public class MainController {

    private final MainService mainService;

    @GetMapping
    @Operation(method = "GET", summary = "메인 화면 조회 API", description = "사용자의 메인 화면 정보를 반환하는 API입니다.")
    public ApiResponse<MainResponseDto> getMain() {
        MainResponseDto result = mainService.getMainPageInfo();
        return ApiResponse.onSuccess(result, "GET_MAIN_SUCCESS", "메인 화면 조회 성공");
    }
}
