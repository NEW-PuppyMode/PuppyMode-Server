package com.umc.puppymode2.global.config.swagger;

import io.swagger.v3.oas.models.examples.Example;
import lombok.Builder;
import lombok.Getter;

/**
 * Swagger 응답 예시를 담는 클래스
 */
@Getter
@Builder
public class ExampleHolder {
    private Example holder;
    private String name;
    private int code;
}