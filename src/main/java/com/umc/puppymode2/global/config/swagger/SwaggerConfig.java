package com.umc.puppymode2.global.config.swagger;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger/OpenAPI 설정 클래스
 *
 * <p>API 문서를 자동으로 생성하고, JWT 인증 및 공통 응답 형식을 정의합니다.</p>
 *
 * <h6>주요 기능:</h6>
 * <ul>
 *   <li>JWT Bearer 인증 스키마 설정</li>
 *   <li>ApiResponse 공통 스키마 등록</li>
 *   <li>에러 응답 자동 생성 ({@link ApiErrorCodeExamples})</li>
 *   <li>성공 응답 자동 생성 ({@link ApiSuccessResponseExample})</li>
 * </ul>
 */
@Configuration
public class SwaggerConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    /**
     * OpenAPI 기본 설정
     */
    @Bean
    public OpenAPI openAPI() {
        Info info = new Info()
                .title("PuppyMode2 API")
                .version("1.0.0")
                .description("강아지 모드2 API 명세서입니다.");

        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList(SECURITY_SCHEME_NAME);

        SecurityScheme securityScheme = new SecurityScheme()
                .name(SECURITY_SCHEME_NAME)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");

        // ApiResponse 공통 스키마 등록
        Schema<?> apiResponseSchema = new ObjectSchema()
                .addProperty("isSuccess", new BooleanSchema()
                        .description("성공 여부")
                        .example(true))
                .addProperty("code", new StringSchema()
                        .description("응답 코드")
                        .example("COMMON200"))
                .addProperty("message", new StringSchema()
                        .description("응답 메시지")
                        .example("성공입니다."))
                .addProperty("result", new ObjectSchema()
                        .nullable(true)
                        .description("응답 데이터"));

        Components components = new Components()
                .addSecuritySchemes(SECURITY_SCHEME_NAME, securityScheme)
                .addSchemas("ApiResponse", apiResponseSchema);

        return new OpenAPI()
                .addServersItem(new Server().url("/").description("현재 서버"))
                .info(info)
                .addSecurityItem(securityRequirement)
                .components(components);
    }

    /**
     * Operation 커스터마이저 체인
     *
     * <p>성공 응답과 에러 응답을 모두 자동으로 생성합니다.</p>
     * <p>실행 순서: 성공 응답 → 에러 응답</p>
     */
    @Bean
    public OperationCustomizer operationCustomizer() {
        // 커스터마이저를 Bean으로 명시적 생성
        SuccessResponseExampleCustomizer successCustomizer = new SuccessResponseExampleCustomizer();
        ApiErrorCodeExampleCustomizer errorCustomizer = new ApiErrorCodeExampleCustomizer();

        return (operation, handlerMethod) -> {
            // 1. 성공 응답 예시 추가
            operation = successCustomizer.customize(operation, handlerMethod);

            // 2. 에러 응답 예시 추가
            operation = errorCustomizer.customize(operation, handlerMethod);

            return operation;
        };
    }
}