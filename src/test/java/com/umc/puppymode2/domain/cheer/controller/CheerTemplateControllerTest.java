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

    private CheerTemplateListResponseDTO templates(String message) {
        return CheerTemplateListResponseDTO.builder()
                .categories(List.of(CheerTemplateListResponseDTO.Category.builder()
                        .category(CheerCategory.COMFORT)
                        .label("위로")
                        .templates(List.of(CheerTemplateListResponseDTO.Template.builder().id(6L).message(message).build()))
                        .build()))
                .build();
    }
}
