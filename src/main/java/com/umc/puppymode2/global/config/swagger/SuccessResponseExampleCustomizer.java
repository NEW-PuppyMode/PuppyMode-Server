package com.umc.puppymode2.global.config.swagger;

import com.umc.puppymode2.global.apiPayload.code.status.SuccessStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * @ApiSuccessResponseExample 어노테이션을 읽어서 Swagger에 성공 응답 예시를 자동으로 생성
 *
 * <p>DTO @Schema 기반 자동화</p>
 * <p>DTO 클래스의 {@link Schema} 어노테이션에 정의된 example 값을 리플렉션으로 읽어서 자동으로 예시를 생성합니다.</p>
 *
 * <h6>주요 기능:</h6>
 * <ul>
 *   <li>SuccessStatus의 code, message 자동 적용</li>
 *   <li>DTO의 @Schema example 값 자동 추출 (리플렉션)</li>
 *   <li>중첩 객체 자동 처리 (예: LoginResponseDTO.LoginUserInfo)</li>
 *   <li>Void 타입 응답 처리 (로그아웃 등)</li>
 *   <li>새 DTO 추가 시 코드 수정 불필요</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SuccessResponseExampleCustomizer implements OperationCustomizer {

    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {

        ApiSuccessResponseExample annotation = handlerMethod.getMethodAnnotation(ApiSuccessResponseExample.class);

        if (annotation == null) {
            return operation;
        }

        try {
            addSuccessResponseExample(operation, annotation);
        } catch (Exception e) {
            log.error("[Swagger] 성공 응답 예시 생성 중 오류 발생 - method: {}, error: {}",
                    handlerMethod.getMethod().getName(), e.getMessage(), e);
            // 예외 발생해도 Swagger 문서 생성 진행
        }

        return operation;
    }

    /**
     * 성공 응답 예시를 Operation에 추가
     */
    private void addSuccessResponseExample(Operation operation, ApiSuccessResponseExample annotation) {

        SuccessStatus status = annotation.status();
        Class<?> responseType = annotation.responseType();

        // ApiResponses 초기화
        ApiResponses responses = operation.getResponses();
        if (responses == null) {
            responses = new ApiResponses();
            operation.setResponses(responses);
        }

        // 200 응답 생성
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setDescription("성공");

        // Content 생성 (application/json 강제)
        Content content = new Content();
        MediaType mediaType = new MediaType();

        // Schema 참조 설정
        io.swagger.v3.oas.models.media.Schema schema = new io.swagger.v3.oas.models.media.Schema();
        schema.set$ref("#/components/schemas/ApiResponse");
        mediaType.setSchema(schema);

        // 성공 응답 예시 생성
        Example example = createSuccessExample(status, responseType);
        mediaType.addExamples(status.name(), example);

        content.addMediaType("application/json", mediaType);
        apiResponse.setContent(content);
        responses.addApiResponse("200", apiResponse);

        log.debug("[Swagger] 성공 응답 예시 추가 완료 - status: {}, responseType: {}",
                status.name(), responseType.getSimpleName());
    }

    /**
     * SuccessStatus와 응답 타입을 기반으로 성공 응답 예시 생성
     */
    private Example createSuccessExample(SuccessStatus status, Class<?> responseType) {

        Map<String, Object> exampleResponse = new HashMap<>();
        exampleResponse.put("isSuccess", true);
        exampleResponse.put("code", status.getCode());
        exampleResponse.put("message", status.getMessage());

        // Void가 아닌 경우에만 result 필드 추가
        if (!responseType.equals(Void.class)) {
            exampleResponse.put("result", generateExampleFromDTO(responseType));
        }

        Example example = new Example();
        example.setSummary(status.name());
        example.setDescription(String.format("Success response with code %s", status.getCode()));
        example.setValue(exampleResponse);

        return example;
    }

    /**
     * DTO 클래스의 @Schema 어노테이션에서 example 값을 추출하여 예시 생성 (리플렉션)
     *
     * <p>DTO 수정 시 자동으로 예시 업데이트됨</p>
     */
    private Object generateExampleFromDTO(Class<?> dtoClass) {
        try {
            // String 타입은 그대로 반환
            if (dtoClass.equals(String.class)) {
                return "회원탈퇴가 완료되었습니다.";
            }

            Map<String, Object> example = new HashMap<>();
            Field[] fields = dtoClass.getDeclaredFields();

            for (Field field : fields) {
                // @Schema 어노테이션 확인
                Schema schema = field.getAnnotation(Schema.class);
                if (schema == null) {
                    continue; // @Schema 없으면 스킵
                }

                String fieldName = field.getName();
                String exampleValue = schema.example();

                // example 값이 있으면 파싱해서 추가
                if (exampleValue != null && !exampleValue.isEmpty()) {
                    example.put(fieldName, parseExampleValue(exampleValue, field.getType()));
                }
                // example 값이 없고 중첩 객체면 재귀 처리
                else if (!isPrimitiveOrWrapper(field.getType())) {
                    example.put(fieldName, generateExampleFromDTO(field.getType()));
                }
            }

            return example;

        } catch (Exception e) {
            log.warn("[Swagger] DTO 예시 생성 실패: {} - 빈 객체 반환", dtoClass.getSimpleName(), e);
            return new HashMap<>();
        }
    }

    /**
     * example 문자열을 적절한 타입으로 파싱
     */
    private Object parseExampleValue(String exampleValue, Class<?> fieldType) {
        try {
            // null 문자열 처리
            if ("null".equalsIgnoreCase(exampleValue)) {
                return null;
            }

            // Boolean
            if (fieldType == Boolean.class || fieldType == boolean.class) {
                return Boolean.parseBoolean(exampleValue);
            }

            // Long
            if (fieldType == Long.class || fieldType == long.class) {
                return Long.parseLong(exampleValue);
            }

            // Integer
            if (fieldType == Integer.class || fieldType == int.class) {
                return Integer.parseInt(exampleValue);
            }

            // String (기본값)
            return exampleValue;

        } catch (Exception e) {
            log.debug("[Swagger] 예시 값 파싱 실패: {} - 문자열로 반환", exampleValue);
            return exampleValue;
        }
    }

    /**
     * 기본 타입 또는 래퍼 클래스인지 확인
     */
    private boolean isPrimitiveOrWrapper(Class<?> type) {
        return type.isPrimitive() ||
                type == String.class ||
                type == Integer.class ||
                type == Long.class ||
                type == Double.class ||
                type == Float.class ||
                type == Boolean.class ||
                type == Character.class ||
                type == Byte.class ||
                type == Short.class;
    }
}