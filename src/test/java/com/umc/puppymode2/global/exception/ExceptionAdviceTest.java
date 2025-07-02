package com.umc.puppymode2.global.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.umc.puppymode2.global.apiPayload.ApiResponse;
import com.umc.puppymode2.global.apiPayload.code.status.ErrorStatus;
import com.umc.puppymode2.global.exception.handler.TempHandler;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@ContextConfiguration(classes = {
        ExceptionAdviceTest.TestController.class,
        ExceptionAdvice.class,
        ExceptionAdviceTest.TestConfig.class
})
class ExceptionAdviceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Configuration
    static class TestConfig {
        // 필요한 빈들 Mock으로 등록
    }

    @RestController
    static class TestController {

        @GetMapping("/test/general-exception")
        public ApiResponse<String> testGeneralException() {
            throw new GeneralException(ErrorStatus._INTERNAL_SERVER_ERROR);
        }

        @GetMapping("/test/temp-exception")
        public ApiResponse<String> testTempException() {
            throw new TempHandler(ErrorStatus.TEMP_EXCEPTION);
        }

        @GetMapping("/test/runtime-exception")
        public ApiResponse<String> testRuntimeException() {
            throw new RuntimeException("테스트 런타임 예외");
        }

        @GetMapping("/test/validation/{id}")
        public ApiResponse<String> testValidation(@PathVariable Long id) {
            if (id <= 0) {
                throw new IllegalArgumentException("ID는 0보다 커야 합니다.");
            }
            return ApiResponse.onSuccess("성공");
        }

        @PostMapping("/test/json-endpoint")
        public ApiResponse<String> testJsonEndpoint(@RequestBody Map<String, Object> data) {
            return ApiResponse.onSuccess("JSON 처리 성공");
        }
    }

    @Test
    @DisplayName("GeneralException 처리 테스트")
    void handleGeneralException() throws Exception {
        mockMvc.perform(get("/test/general-exception"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON500"))
                .andExpect(jsonPath("$.message").value("서버 에러, 관리자에게 문의 바랍니다."));
    }

    @Test
    @DisplayName("TempHandler 예외 처리 테스트")
    void handleTempException() throws Exception {
        mockMvc.perform(get("/test/temp-exception"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("TEMP4001"))
                .andExpect(jsonPath("$.message").value("테스트 에러입니다.")); // 실제 메시지로 수정
    }

    @Test
    @DisplayName("RuntimeException 처리 테스트")
    void handleRuntimeException() throws Exception {
        mockMvc.perform(get("/test/runtime-exception"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON500"));
    }

    @Test
    @DisplayName("IllegalArgumentException 처리 테스트")
    void handleIllegalArgumentException() throws Exception {
        mockMvc.perform(get("/test/validation/-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.message").value("ID는 0보다 커야 합니다."));
    }

    @Test
    @DisplayName("404 Not Found 처리 테스트")
    void handleNotFound() throws Exception {
        mockMvc.perform(get("/test/not-existing-endpoint"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("405 Method Not Allowed 처리 테스트")
    void handleMethodNotAllowed() throws Exception {
        mockMvc.perform(post("/test/general-exception"))  // GET 엔드포인트에 POST 요청
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("잘못된 JSON 형식 처리 테스트")
    void handleInvalidJson() throws Exception {
        String invalidJson = "{ invalid json }";

        mockMvc.perform(post("/test/json-endpoint")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }
}