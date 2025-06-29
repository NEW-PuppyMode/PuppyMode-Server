package com.umc.puppymode2.domain.temp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.umc.puppymode2.domain.temp.entity.enums.TempStatus;

import java.time.LocalDateTime;
import java.util.List;

public class TempResponseDTO {

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "임시 데이터 생성 결과")
    public static class CreateTempResultDTO {
        @Schema(description = "생성된 임시 데이터 ID", example = "1")
        private Long tempId;

        @Schema(description = "생성 시간")
        private LocalDateTime createdAt;
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "임시 데이터 정보")
    public static class TempDTO {
        @Schema(description = "임시 데이터 ID", example = "1")
        private Long tempId;

        @Schema(description = "임시 데이터 이름", example = "테스트 데이터")
        private String name;

        @Schema(description = "임시 데이터 설명", example = "이것은 테스트용 임시 데이터입니다.")
        private String description;

        @Schema(description = "임시 데이터 상태", example = "ACTIVE")
        private TempStatus status;

        @Schema(description = "생성 시간")
        private LocalDateTime createdAt;

        @Schema(description = "수정 시간")
        private LocalDateTime updatedAt;
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "임시 데이터 목록")
    public static class TempListDTO {
        @Schema(description = "임시 데이터 목록")
        private List<TempDTO> tempList;

        @Schema(description = "총 개수", example = "10")
        private Integer listSize;
    }
}
