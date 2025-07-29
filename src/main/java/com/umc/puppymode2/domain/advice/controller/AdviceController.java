package com.umc.puppymode2.domain.advice.controller;

import com.umc.puppymode2.domain.advice.dto.AdviceResponseDTO;
import com.umc.puppymode2.domain.advice.service.AdviceQueryService;
import com.umc.puppymode2.global.apiPayload.ApiResponse;
import com.umc.puppymode2.global.apiPayload.code.status.SuccessStatus;
import com.umc.puppymode2.global.auth.context.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@Tag(name = "advice-controller", description = "한마디 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/advice")
public class AdviceController {

    private final UserContext userContext;
    private final AdviceQueryService adviceQueryService;

    @Operation(summary = "한마디 조회", description = "한마디를 조회하는 API 입니다.")
    @GetMapping
    public ApiResponse<AdviceResponseDTO> getAdvice() {
        Long userId = userContext.getCurrentUserId();
        AdviceResponseDTO advice =  adviceQueryService.getAdvice(userId);
        return ApiResponse.onSuccess(advice, SuccessStatus.ADVICE_GET_SUCCESS.getCode(), SuccessStatus.ADVICE_GET_SUCCESS.getMessage());
    }
}
