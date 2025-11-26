package com.umc.puppymode2.domain.version.controller;

import com.umc.puppymode2.domain.version.dto.VersionResponseDto;
import com.umc.puppymode2.domain.version.service.VersionQueryService;
import com.umc.puppymode2.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/version")
@RequiredArgsConstructor
public class VersionController {

    private final VersionQueryService versionQueryService;

    @Operation(
            summary = "앱 최신 버전 조회",
            description = "현재 앱의 최신 버전 정보를 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "버전 정보 조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "지원하지 않는 OS 타입"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 오류"
            )
    })
    @GetMapping("/check")
    public ResponseEntity<ApiResponse<VersionResponseDto>> checkVersion(
            @Parameter(description = "OS 타입 (Android 또는 iOS)", example = "Android")
            @RequestParam(defaultValue = "Android") String osType) {

        VersionResponseDto response = versionQueryService.getVersionInfo(osType);
        return ResponseEntity.ok(
                ApiResponse.onSuccess(response, "GET_VERSION_INFO", "버전 정보 조회 성공")
        );
    }
}