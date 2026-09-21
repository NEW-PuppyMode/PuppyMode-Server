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

    /**
     * ETag 계산용 문자열: 응답 본문에 나가는 값(분류, 탭 이름, 문구 ID, 문구 내용)과 그 순서를 모두 포함한다.
     * 응답에 보이는 값이 하나라도 다르면 ETag도 달라져야, 바뀐 내용 대신 304를 받는 일이 없다.
     *
     * 각 값은 "길이:값" 형태(길이 접두사)로 이어 붙인다. 문구 안에 구분자로 쓸 만한 문자(; : | 등)가
     * 들어 있어도 필드 경계가 모호해지지 않는다. 예: id=1, message="a;2:b" 인 문구 1개와
     * id=1 "a" / id=2 "b" 인 문구 2개는 서로 다른 문자열이 된다.
     */
    String toEtagSource(CheerTemplateListResponseDTO dto) {
        StringBuilder source = new StringBuilder();
        for (CheerTemplateListResponseDTO.Category category : dto.getCategories()) {
            appendField(source, String.valueOf(category.getCategory()));
            appendField(source, category.getLabel());
            // 문구 개수도 넣어 분류 사이의 경계를 분명히 한다.
            appendField(source, String.valueOf(category.getTemplates().size()));
            for (CheerTemplateListResponseDTO.Template template : category.getTemplates()) {
                appendField(source, String.valueOf(template.getId()));
                appendField(source, template.getMessage());
            }
        }
        return source.toString();
    }

    // 값을 "길이:값;" 으로 붙인다. 길이를 먼저 적기 때문에 값에 어떤 문자가 들어 있어도 경계가 유일하게 결정된다.
    private void appendField(StringBuilder source, String value) {
        String text = String.valueOf(value);
        source.append(text.length()).append(':').append(text).append(';');
    }
}
