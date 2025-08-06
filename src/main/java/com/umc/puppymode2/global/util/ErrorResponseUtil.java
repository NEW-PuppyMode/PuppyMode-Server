package com.umc.puppymode2.global.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.puppymode2.global.apiPayload.ApiResponse;
import com.umc.puppymode2.global.apiPayload.code.status.ErrorStatus;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * 예외 발생 시 json 에러 응답을 생성합니다.
 */
public class ErrorResponseUtil {

    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * ErrorStatus를 기반으로 json 에러 응답을 생성합니다.
     *
     * @param response
     * @param errorStatus
     * @throws IOException
     */
    public static void writeErrorResponse(HttpServletResponse response, ErrorStatus errorStatus) throws IOException {
        response.setStatus(errorStatus.getHttpStatus().value());
        response.setContentType("application/json;charset=UTF-8");

        ApiResponse<Void> errorResponse = ApiResponse.onFailure(
                errorStatus.getCode(),
                errorStatus.getMessage(),
                null
        );

        String json = mapper.writeValueAsString(errorResponse);
        response.getWriter().write(json);
    }
}