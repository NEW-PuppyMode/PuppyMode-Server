package com.umc.puppymode2.domain.cheer.dto;

import com.umc.puppymode2.domain.cheer.entity.enums.CheerCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheerTemplateListResponseDTO {

    private List<Category> categories;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Category {
        private CheerCategory category; // PRANK / COMFORT / CHEER
        private String label;           // 화면 탭 이름 (장난 / 위로 / 응원)
        private List<Template> templates;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Template {
        private Long id;        // 응원 보내기 시 사용하는 문구 ID
        private String message;
    }
}
