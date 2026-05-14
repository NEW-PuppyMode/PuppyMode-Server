package com.umc.puppymode2.domain.version.controller;

import com.umc.puppymode2.domain.version.dto.VersionResponseDto;
import com.umc.puppymode2.domain.version.service.VersionQueryService;
import com.umc.puppymode2.global.apiPayload.ApiResponse;
import com.umc.puppymode2.global.apiPayload.code.status.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "version-controller", description = "앱 버전 조회 API")
@RestController
@RequestMapping("/version")
@RequiredArgsConstructor
public class VersionController {

    private final VersionQueryService versionQueryService;

    @Operation(
            summary = "앱 버전 업데이트 여부 조회",
            description = "현재 앱 버전을 전달하면 강제/선택 업데이트 여부를 반환합니다.\n\n" +
                    "- updateRequired: true → 강제 업데이트 (최소 요구 버전 미만)\n" +
                    "- updateAvailable: true → 선택 업데이트 (최신 버전 존재)\n" +
                    "- 둘 다 false → 최신 버전"
    )
    @GetMapping("/check")
    public ResponseEntity<ApiResponse<VersionResponseDto>> checkVersion(
            @Parameter(description = "OS 타입 (Android 또는 iOS)", example = "Android")
            @RequestParam(defaultValue = "Android") String osType,
            @Parameter(description = "현재 앱 버전", example = "1.0.0")
            @RequestParam String currentVersion) {

        VersionResponseDto response = versionQueryService.getVersionInfo(osType, currentVersion);
        return ResponseEntity.ok(
                ApiResponse.onSuccess(response,
                        SuccessStatus.VERSION_CHECK_SUCCESS.getCode(),
                        SuccessStatus.VERSION_CHECK_SUCCESS.getMessage())
        );
    }
}