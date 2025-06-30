package com.umc.puppymode2.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import com.umc.puppymode2.domain.temp.dto.TempRequestDTO;
import com.umc.puppymode2.domain.temp.entity.Temp;
import com.umc.puppymode2.domain.temp.entity.enums.TempStatus;
import com.umc.puppymode2.domain.temp.repository.TempRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Transactional
class TempIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TempRepository tempRepository;

    @Test
    @DisplayName("Temp 생성부터 조회까지 전체 플로우 테스트")
    void tempFullFlow() throws Exception {
        // given
        TempRequestDTO.CreateTempDTO request = TempRequestDTO.CreateTempDTO.builder()
                .name("통합테스트")
                .description("통합테스트 설명")
                .status(TempStatus.ACTIVE)
                .build();

        // when - Temp 생성
        String createResponse = mockMvc.perform(post("/temp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // 생성된 ID 추출
        JsonNode responseJson = objectMapper.readTree(createResponse);
        Long tempId = responseJson.get("result").get("tempId").asLong();

        // 데이터베이스에서 실제로 생성되었는지 확인
        Temp savedTemp = tempRepository.findById(tempId).orElse(null);
        assertThat(savedTemp).isNotNull();
        assertThat(savedTemp.getName()).isEqualTo("통합테스트");
        assertThat(savedTemp.getDescription()).isEqualTo("통합테스트 설명");
        assertThat(savedTemp.getStatus()).isEqualTo(TempStatus.ACTIVE);

        // then - 생성된 Temp 조회
        mockMvc.perform(get("/temp/{tempId}", tempId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.tempId").value(tempId))
                .andExpect(jsonPath("$.result.name").value("통합테스트"))
                .andExpect(jsonPath("$.result.description").value("통합테스트 설명"))
                .andExpect(jsonPath("$.result.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("존재하지 않는 Temp 조회 시 예외 처리 테스트")
    void getTempNotFound() throws Exception {
        // given
        Long nonExistentId = 999L;

        // when & then
        mockMvc.perform(get("/temp/{tempId}", nonExistentId))
                .andExpect(status().isNotFound()) // 이제 404가 정상적으로 반환됨
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("TEMP4041"))
                .andExpect(jsonPath("$.message").value("임시 데이터가 존재하지 않습니다."));
    }

    @Test
    @DisplayName("잘못된 요청으로 Temp 생성 시 예외 처리 테스트")
    void createTempWithInvalidRequest() throws Exception {
        // given - name이 비어있는 잘못된 요청
        TempRequestDTO.CreateTempDTO invalidRequest = TempRequestDTO.CreateTempDTO.builder()
                .name("") // 빈 문자열
                .status(TempStatus.ACTIVE)
                .build();

        // when & then
        mockMvc.perform(post("/temp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("status 없이 Temp 생성 시 예외 처리 테스트")
    void createTempWithoutStatus() throws Exception {
        // given - status가 없는 잘못된 요청
        TempRequestDTO.CreateTempDTO invalidRequest = TempRequestDTO.CreateTempDTO.builder()
                .name("테스트")
                .description("설명")
                // .status() 없음
                .build();

        // when & then
        mockMvc.perform(post("/temp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}