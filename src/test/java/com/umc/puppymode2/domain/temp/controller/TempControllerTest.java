package com.umc.puppymode2.domain.temp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.puppymode2.domain.temp.dto.TempRequestDTO;
import com.umc.puppymode2.domain.temp.entity.Temp;
import com.umc.puppymode2.domain.temp.entity.enums.TempStatus;
import com.umc.puppymode2.domain.temp.service.TempCommandService;
import com.umc.puppymode2.domain.temp.service.TempQueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TempController.class)
@Import(TempControllerTest.MockConfig.class)
class TempControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TempCommandService tempCommandService;

    @Autowired
    private TempQueryService tempQueryService;

    @TestConfiguration
    static class MockConfig {
        @Bean
        @Primary
        public TempCommandService tempCommandService() {
            return Mockito.mock(TempCommandService.class);
        }

        @Bean
        @Primary
        public TempQueryService tempQueryService() {
            return Mockito.mock(TempQueryService.class);
        }
    }

    @Test
    @DisplayName("Temp 생성 API 테스트")
    void createTemp() throws Exception {
        // given
        TempRequestDTO.CreateTempDTO request = TempRequestDTO.CreateTempDTO.builder()
                .name("테스트")
                .description("설명입니다")
                .status(TempStatus.ACTIVE)
                .build();

        Long createdId = 1L;

        given(tempCommandService.createTemp(any(TempRequestDTO.CreateTempDTO.class)))
                .willReturn(createdId);

        // when & then
        mockMvc.perform(post("/temp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.tempId").value(1L));
    }

    @Test
    @DisplayName("Temp 단건 조회 API 테스트")
    void getTemp() throws Exception {
        // given
        Long tempId = 1L;
        LocalDateTime now = LocalDateTime.of(2024, 1, 1, 12, 0);

        Temp temp = Temp.builder()
                .id(tempId)
                .name("테스트")
                .description("설명입니다")
                .status(TempStatus.ACTIVE)
                .build();

        // createdAt, updatedAt 값 리플렉션으로 설정
        setField(temp, "createdAt", now);
        setField(temp, "updatedAt", now);

        given(tempQueryService.findTempById(tempId)).willReturn(temp);

        // when & then
        mockMvc.perform(get("/temp/{tempId}", tempId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.tempId").value(tempId))
                .andExpect(jsonPath("$.result.name").value("테스트"))
                .andExpect(jsonPath("$.result.description").value("설명입니다"))
                .andExpect(jsonPath("$.result.status").value("ACTIVE"))
                .andExpect(jsonPath("$.result.createdAt").value("2024-01-01T12:00:00"))
                .andExpect(jsonPath("$.result.updatedAt").value("2024-01-01T12:00:00"));
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getSuperclass().getDeclaredField(fieldName); // BaseEntity
        field.setAccessible(true);
        field.set(target, value);
    }
}