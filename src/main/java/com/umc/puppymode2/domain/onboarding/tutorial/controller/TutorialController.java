package com.umc.puppymode2.domain.onboarding.tutorial.controller;

import com.umc.puppymode2.domain.onboarding.tutorial.service.TutorialService;
import com.umc.puppymode2.global.apiPayload.ApiResponse;
import com.umc.puppymode2.global.apiPayload.code.status.ErrorStatus;
import com.umc.puppymode2.global.apiPayload.code.status.SuccessStatus;
import com.umc.puppymode2.global.auth.context.UserContext;
import com.umc.puppymode2.global.config.swagger.ApiErrorCodeExamples;
import com.umc.puppymode2.global.config.swagger.ApiSuccessResponseExample;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/onboarding/tutorial")
@Tag(name = "onboarding", description = "온보딩/튜토리얼 관련 API")
public class TutorialController {

    private final TutorialService tutorialService;
    private final UserContext userContext;

    /**
     * 튜토리얼 진행 상태 등록
     *
     * 프론트에서 튜토리얼 화면 진입하는 시점에 호출
     * 이후 유저가 끝까지 다 보든, 중간에 앱을 나가든 상관없이
     * 다음 GET /auth/me 조회 시 tutorialShown이 true로
     * 튜토리얼이 다시 노출되지 않음. (중도 이탈 = 자동 스킵)
     */
    @Operation(
            summary = "튜토리얼 상태 등록",
            description = """
                    튜토리얼 화면 진입 시 호출하여 진행 상태를 '봤음'으로 등록.
                    
                    - 호출 시점: 튜토리얼 화면이 처음 렌더링될 때 (다 본 후 x)
                    - 멱등성: 이미 등록된 상태에서 재호출해도 200 응답
                    """
    )
    @PostMapping("/start")
    @ApiSuccessResponseExample(
            status = SuccessStatus.TUTORIAL_START_SUCCESS,
            responseType = Void.class
    )
    @ApiErrorCodeExamples({
            ErrorStatus.AUTH_INVALID_TOKEN,
            ErrorStatus.USER_NOT_FOUND
    })
    public ApiResponse<Void> start() {
        Long userId = userContext.getCurrentUserId();
        tutorialService.markTutorialShown(userId);

        return ApiResponse.onSuccess(
                null,
                SuccessStatus.TUTORIAL_START_SUCCESS.getCode(),
                SuccessStatus.TUTORIAL_START_SUCCESS.getMessage()
        );
    }
}

