package com.umc.puppymode2.domain.onboarding.controller;

import com.umc.puppymode2.domain.onboarding.dto.OnboardingTestReqDTO;
import com.umc.puppymode2.domain.onboarding.dto.OnboardingTestResDTO;
import com.umc.puppymode2.domain.onboarding.service.OnboardingTestService;
import com.umc.puppymode2.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/onboarding")
public class OnboardingController {

    private final OnboardingTestService onboardingTestService;

    @PostMapping("/test")
    @Operation(method = "POST", summary = "사용자 선택지에 따른 강아지 추천 API", description = "사용자의 선택지에 따른 강아지를 추천 및 객체 생성해주는 API입니다.")
    public ApiResponse<OnboardingTestResDTO> recommendAndCreatePuppy(
            @Valid @RequestBody OnboardingTestReqDTO onboardingTestReqDTO) {

        return ApiResponse.onSuccess(onboardingTestService.recommendAndCreatePuppy(onboardingTestReqDTO));
    }
}
