package com.umc.puppymode2.domain.cheer.converter;

import com.umc.puppymode2.domain.cheer.dto.CheerTemplateListResponseDTO;
import com.umc.puppymode2.domain.cheer.entity.CheerTemplate;
import com.umc.puppymode2.domain.cheer.entity.enums.CheerCategory;
import com.umc.puppymode2.domain.friend.converter.FriendConverter;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CheerConverterTest {

    private final CheerConverter converter = new CheerConverter(new FriendConverter());

    @Test
    void 문구는_분류별로_묶이고_분류는_항상_장난_위로_응원_순서다() {
        // 입력 순서는 섞여 있어도 분류 순서는 enum 선언 순서로 고정된다.
        List<CheerTemplate> templates = List.of(
                template(11L, CheerCategory.CHEER, "그럴 수도 있어 다음이 중요해"),
                template(1L, CheerCategory.PRANK, "이정도면 알콜 중독이야"),
                template(6L, CheerCategory.COMFORT, "그래 마실 수도 있지"));

        CheerTemplateListResponseDTO result = converter.toTemplateListDto(templates);

        assertEquals(List.of(CheerCategory.PRANK, CheerCategory.COMFORT, CheerCategory.CHEER),
                result.getCategories().stream().map(CheerTemplateListResponseDTO.Category::getCategory).toList());
        assertEquals(List.of("장난", "위로", "응원"),
                result.getCategories().stream().map(CheerTemplateListResponseDTO.Category::getLabel).toList());
    }

    @Test
    void 분류_안에서는_입력된_순서_display_order를_유지한다() {
        List<CheerTemplate> templates = List.of(
                template(6L, CheerCategory.COMFORT, "그래 마실 수도 있지"),
                template(7L, CheerCategory.COMFORT, "너무 자책하지마"));

        CheerTemplateListResponseDTO.Category comfort = converter.toTemplateListDto(templates).getCategories().get(1);

        assertEquals(List.of(6L, 7L),
                comfort.getTemplates().stream().map(CheerTemplateListResponseDTO.Template::getId).toList());
        assertEquals("그래 마실 수도 있지", comfort.getTemplates().get(0).getMessage());
    }

    @Test
    void 문구가_없는_분류도_빈_배열로_내려간다() {
        CheerTemplateListResponseDTO result = converter.toTemplateListDto(List.of());

        assertEquals(3, result.getCategories().size());
        assertTrue(result.getCategories().stream().allMatch(c -> c.getTemplates().isEmpty()));
    }

    private CheerTemplate template(Long id, CheerCategory category, String message) {
        CheerTemplate template = CheerTemplate.of(category, message, 1);
        ReflectionTestUtils.setField(template, "cheerTemplateId", id);
        return template;
    }
}
