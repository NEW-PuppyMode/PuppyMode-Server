package com.umc.puppymode2.domain.cheer.controller;

import com.umc.puppymode2.domain.cheer.dto.CheerTemplateListResponseDTO;
import com.umc.puppymode2.domain.cheer.entity.enums.CheerCategory;
import com.umc.puppymode2.domain.cheer.service.CheerTemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CheerTemplateControllerTest {

    @Mock
    private CheerTemplateService cheerTemplateService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new CheerTemplateController(cheerTemplateService)).build();
    }

    @Test
    void 응원_문구_조회는_ETag와_Cache_Control을_내려준다() throws Exception {
        when(cheerTemplateService.getTemplates()).thenReturn(templates("그래 마실 수도 있지"));

        var result = mockMvc.perform(get("/cheer-templates"))
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"))
                .andExpect(jsonPath("$.code").value("CHEER200"))
                .andExpect(jsonPath("$.result.categories[0].category").value("COMFORT"))
                .andExpect(jsonPath("$.result.categories[0].templates[0].message").value("그래 마실 수도 있지"))
                .andReturn();

        String cacheControl = result.getResponse().getHeader("Cache-Control");
        assertNotNull(cacheControl);
        assertTrue(cacheControl.contains("max-age=600"), cacheControl);
        assertTrue(cacheControl.contains("private"), cacheControl);
    }

    @Test
    void If_None_Match가_일치하면_304이고_본문이_없다() throws Exception {
        when(cheerTemplateService.getTemplates()).thenReturn(templates("그래 마실 수도 있지"));
        String etag = mockMvc.perform(get("/cheer-templates")).andReturn().getResponse().getHeader("ETag");

        mockMvc.perform(get("/cheer-templates").header("If-None-Match", etag))
                .andExpect(status().isNotModified())
                .andExpect(content().string(""));
    }

    @Test
    void 문구가_바뀌면_ETag가_달라져_새_본문을_내려준다() throws Exception {
        when(cheerTemplateService.getTemplates())
                .thenReturn(templates("그래 마실 수도 있지"))
                .thenReturn(templates("문구가 바뀌었어요"));
        String oldEtag = mockMvc.perform(get("/cheer-templates")).andReturn().getResponse().getHeader("ETag");

        var result = mockMvc.perform(get("/cheer-templates").header("If-None-Match", oldEtag))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.categories[0].templates[0].message").value("문구가 바뀌었어요"))
                .andReturn();

        assertNotEquals(oldEtag, result.getResponse().getHeader("ETag"));
    }

    @Test
    void 같은_내용이면_ETag가_같다() throws Exception {
        when(cheerTemplateService.getTemplates()).thenReturn(templates("그래 마실 수도 있지"));

        String first = mockMvc.perform(get("/cheer-templates")).andReturn().getResponse().getHeader("ETag");
        String second = mockMvc.perform(get("/cheer-templates")).andReturn().getResponse().getHeader("ETag");

        assertEquals(first, second);
    }

    @Test
    void 탭_이름_label만_바뀌어도_ETag가_달라져_304를_받지_않는다() throws Exception {
        when(cheerTemplateService.getTemplates())
                .thenReturn(templates("위로", "그래 마실 수도 있지"))
                .thenReturn(templates("위로해요", "그래 마실 수도 있지"));
        String oldEtag = mockMvc.perform(get("/cheer-templates")).andReturn().getResponse().getHeader("ETag");

        mockMvc.perform(get("/cheer-templates").header("If-None-Match", oldEtag))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.categories[0].label").value("위로해요"));
    }

    @Test
    void 문구에_구분자가_들어_있어도_서로_다른_목록은_ETag_입력이_다르다() {
        CheerTemplateController controller = new CheerTemplateController(cheerTemplateService);

        // 예전 방식(| : ; 로 단순 연결)이면 두 목록 모두 "COMFORT|1:a;2:b;" 로 같아지던 경우
        CheerTemplateListResponseDTO oneTemplate = list(
                List.of(template(1L, "a;2:b")));
        CheerTemplateListResponseDTO twoTemplates = list(
                List.of(template(1L, "a"), template(2L, "b")));

        assertNotEquals(controller.toEtagSource(oneTemplate), controller.toEtagSource(twoTemplates));
    }

    @Test
    void 분류_사이에서_문구가_이동해도_ETag_입력이_달라진다() {
        CheerTemplateController controller = new CheerTemplateController(cheerTemplateService);

        CheerTemplateListResponseDTO before = CheerTemplateListResponseDTO.builder().categories(List.of(
                category(CheerCategory.PRANK, "장난", List.of(template(1L, "a"))),
                category(CheerCategory.COMFORT, "위로", List.of()))).build();
        CheerTemplateListResponseDTO after = CheerTemplateListResponseDTO.builder().categories(List.of(
                category(CheerCategory.PRANK, "장난", List.of()),
                category(CheerCategory.COMFORT, "위로", List.of(template(1L, "a"))))).build();

        assertNotEquals(controller.toEtagSource(before), controller.toEtagSource(after));
    }

    @Test
    void 응답에_보이는_값이_모두_같으면_ETag_입력도_같다() {
        CheerTemplateController controller = new CheerTemplateController(cheerTemplateService);

        assertEquals(
                controller.toEtagSource(templates("위로", "그래 마실 수도 있지")),
                controller.toEtagSource(templates("위로", "그래 마실 수도 있지")));
    }

    private CheerTemplateListResponseDTO.Template template(Long id, String message) {
        return CheerTemplateListResponseDTO.Template.builder().id(id).message(message).build();
    }

    private CheerTemplateListResponseDTO.Category category(CheerCategory category, String label,
                                                           List<CheerTemplateListResponseDTO.Template> templates) {
        return CheerTemplateListResponseDTO.Category.builder()
                .category(category).label(label).templates(templates).build();
    }

    private CheerTemplateListResponseDTO list(List<CheerTemplateListResponseDTO.Template> templates) {
        return CheerTemplateListResponseDTO.builder()
                .categories(List.of(category(CheerCategory.COMFORT, "위로", templates)))
                .build();
    }

    private CheerTemplateListResponseDTO templates(String message) {
        return templates("위로", message);
    }

    private CheerTemplateListResponseDTO templates(String label, String message) {
        return CheerTemplateListResponseDTO.builder()
                .categories(List.of(CheerTemplateListResponseDTO.Category.builder()
                        .category(CheerCategory.COMFORT)
                        .label(label)
                        .templates(List.of(CheerTemplateListResponseDTO.Template.builder().id(6L).message(message).build()))
                        .build()))
                .build();
    }
}
