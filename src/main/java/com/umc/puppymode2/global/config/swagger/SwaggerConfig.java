package com.umc.puppymode2.global.config.swagger;

import com.umc.puppymode2.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

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

        // ApiResponse 스키마 수동 등록
        Schema<?> apiResponseSchema = new ObjectSchema()
                .addProperty("isSuccess", new BooleanSchema().description("성공 여부").example(false))
                .addProperty("code", new StringSchema().description("응답 코드").example("COMMON500"))
                .addProperty("message", new StringSchema().description("응답 메시지").example("서버 에러"))
                .addProperty("result", new ObjectSchema().nullable(true).description("응답 데이터"));

        Components components = new Components()
                .addSecuritySchemes(SECURITY_SCHEME_NAME, securityScheme)
                .addSchemas("ApiResponse", apiResponseSchema);

        return new OpenAPI()
                .addServersItem(new Server().url("/"))
                .info(info)
                .addSecurityItem(securityRequirement)
                .components(components);
    }

    /**
     * @ApiErrorCodeExamples 어노테이션을 처리하는 커스터마이저
     */
    @Bean
    public OperationCustomizer operationCustomizer() {
        return new ApiErrorCodeExampleCustomizer();
    }
}