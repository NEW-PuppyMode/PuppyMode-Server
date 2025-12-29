package com.umc.puppymode2.global.config.swagger;

import com.umc.puppymode2.global.apiPayload.code.status.ErrorStatus;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

import java.util.*;

/**
 * @ApiErrorCodeExamples 어노테이션을 읽어서 Swagger에 에러 응답을 자동으로 추가
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiErrorCodeExampleCustomizer implements OperationCustomizer {

    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {

        ApiErrorCodeExamples annotation = handlerMethod.getMethodAnnotation(ApiErrorCodeExamples.class);

        if (annotation == null) {
            return operation;
        }

        List<ErrorStatus> errorStatuses = Arrays.asList(annotation.value());

        if (errorStatuses.isEmpty()) {
            return operation;
        }

        ApiResponses responses = operation.getResponses();
        if (responses == null) {
            responses = new ApiResponses();
            operation.setResponses(responses);
        }

        for (ErrorStatus errorStatus : errorStatuses) {
            String statusCode = String.valueOf(errorStatus.getReasonHttpStatus().getHttpStatus().value());
            ExampleHolder holder = createExampleHolder(errorStatus);

            ApiResponse apiResponse = responses.computeIfAbsent(statusCode, code -> new ApiResponse());

            if (apiResponse.getDescription() == null) {
                apiResponse.setDescription("Error Response");
            }

            Content content = apiResponse.getContent();
            if (content == null) {
                content = new Content();
                apiResponse.setContent(content);
            }

            MediaType mediaType = content.get("application/json");
            if (mediaType == null) {
                mediaType = new MediaType();
                // 제네릭 없이 raw 타입 Schema 생성 후 $ref 설정 -> 스웨거가 components/schemas/ApiResponse 정상 참조
                Schema schema = new Schema();
                schema.set$ref("#/components/schemas/ApiResponse");
                mediaType.setSchema(schema);
                content.addMediaType("application/json", mediaType);
            }

            mediaType.addExamples(holder.getName(), holder.getHolder());
        }

        return operation;
    }

    /**
     * ErrorStatus로부터 ExampleHolder 생성
     */
    private ExampleHolder createExampleHolder(ErrorStatus errorStatus) {
        // ApiResponse.onFailure() 사용
        com.umc.puppymode2.global.apiPayload.ApiResponse<Object> errorResponse =
                com.umc.puppymode2.global.apiPayload.ApiResponse.onFailure(
                        errorStatus.getCode(),
                        errorStatus.getMessage(),
                        null
                );

        Example example = new Example();
        example.setSummary(errorStatus.name());
        example.setDescription(errorStatus.getMessage());
        example.setValue(errorResponse);

        return ExampleHolder.builder()
                .holder(example)
                .name(errorStatus.name())
                .code(errorStatus.getReasonHttpStatus().getHttpStatus().value())
                .build();
    }
}