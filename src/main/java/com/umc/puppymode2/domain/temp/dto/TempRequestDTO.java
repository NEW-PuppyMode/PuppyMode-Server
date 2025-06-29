package com.umc.puppymode2.domain.temp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import com.umc.puppymode2.domain.temp.entity.enums.TempStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class TempRequestDTO {

    @Getter
    @Schema(description = "임시 데이터 생성 요청")
    public static class CreateTempDTO {

        @NotBlank(message = "이름은 필수입니다.")
        @Size(max = 100, message = "이름은 100자 이내여야 합니다.")
        @Schema(description = "임시 데이터 이름", example = "테스트 데이터")
        private String name;

        @Size(max = 500, message = "설명은 500자 이내여야 합니다.")
        @Schema(description = "임시 데이터 설명", example = "이것은 테스트용 임시 데이터입니다.")
        private String description;

        @NotNull(message = "상태는 필수입니다.")
        @Schema(description = "임시 데이터 상태", example = "ACTIVE")
        private TempStatus status;
    }

    @Getter
    @Schema(description = "임시 데이터 수정 요청")
    public static class UpdateTempDTO {

        @Size(max = 500, message = "설명은 500자 이내여야 합니다.")
        @Schema(description = "임시 데이터 설명", example = "수정된 설명입니다.")
        private String description;

        @Schema(description = "임시 데이터 상태", example = "INACTIVE")
        private TempStatus status;
    }
}
