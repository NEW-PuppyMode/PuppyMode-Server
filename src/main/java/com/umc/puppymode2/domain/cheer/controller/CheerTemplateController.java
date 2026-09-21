package com.umc.puppymode2.domain.cheer.controller;

import com.umc.puppymode2.domain.cheer.dto.CheerTemplateListResponseDTO;
import com.umc.puppymode2.domain.cheer.service.CheerTemplateService;
import com.umc.puppymode2.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@RestController
@RequestMapping("/cheer-templates")
@RequiredArgsConstructor
public class CheerTemplateController {

    // 문구는 자주 바뀌지 않으므로 클라이언트가 잠시 재사용하게 한다. (이후에는 ETag로 변경 여부만 확인)
    private static final Duration CACHE_MAX_AGE = Duration.ofMinutes(10);

    private final CheerTemplateService cheerTemplateService;

    @GetMapping
    @Operation(summary = "응원 문구 조회 API",
            description = "활성 응원 문구를 분류(장난/위로/응원)별로 반환합니다. 각 분류는 display_order 순입니다. " +
                    "ETag/Cache-Control을 내려주며, If-None-Match가 일치하면 304를 반환합니다.")
    public ResponseEntity<ApiResponse<CheerTemplateListResponseDTO>> getCheerTemplates(WebRequest request) {
        CheerTemplateListResponseDTO response = cheerTemplateService.getTemplates();

        // 문구 내용으로 ETag를 만든다. 문구가 바뀌면 값이 달라지고, 그대로면 304로 본문 전송을 생략한다.
        String etag = "\"" + DigestUtils.md5DigestAsHex(toEtagSource(response).getBytes(StandardCharsets.UTF_8)) + "\"";
        if (request.checkNotModified(etag)) {
            // 304 응답은 checkNotModified가 이미 설정했으므로 본문 없이 종료한다.
            return null;
        }

        return ResponseEntity.ok()
                .eTag(etag)
                .cacheControl(CacheControl.maxAge(CACHE_MAX_AGE).cachePrivate())
                .body(ApiResponse.onSuccess(response, "CHEER200", "응원 문구 조회 성공"));
    }

    // ETag 계산용 문자열: 분류, 문구 ID, 문구 내용의 순서까지 포함해 내용이 하나라도 다르면 달라지게 한다.
    private String toEtagSource(CheerTemplateListResponseDTO dto) {
        StringBuilder source = new StringBuilder();
        for (CheerTemplateListResponseDTO.Category category : dto.getCategories()) {
            source.append(category.getCategory()).append('|');
            for (CheerTemplateListResponseDTO.Template template : category.getTemplates()) {
                source.append(template.getId()).append(':').append(template.getMessage()).append(';');
            }
        }
        return source.toString();
    }
}
