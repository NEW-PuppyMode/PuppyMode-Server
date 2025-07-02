package com.umc.puppymode2.domain.temp.converter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.umc.puppymode2.domain.temp.dto.TempRequestDTO;
import com.umc.puppymode2.domain.temp.dto.TempResponseDTO;
import com.umc.puppymode2.domain.temp.entity.Temp;
import com.umc.puppymode2.domain.temp.entity.enums.TempStatus;

import static org.assertj.core.api.Assertions.*;

class TempConverterTest {

    private final TempConverter tempConverter = new TempConverter();

    @Test
    @DisplayName("CreateTempDTO를 Temp 엔티티로 변환 테스트")
    void toTemp() {
        // given
        TempRequestDTO.CreateTempDTO request = TempRequestDTO.CreateTempDTO.builder()
                .name("테스트")
                .status(TempStatus.ACTIVE)
                .build();

        // when
        Temp temp = tempConverter.toTemp(request);

        // then
        assertThat(temp.getName()).isEqualTo("테스트");
        assertThat(temp.getStatus()).isEqualTo(TempStatus.ACTIVE);
    }

    @Test
    @DisplayName("Temp 엔티티를 TempDTO로 변환 테스트")
    void toTempDto() {
        // given
        Temp temp = Temp.builder()
                .id(1L)
                .name("테스트")
                .status(TempStatus.ACTIVE)
                .build();

        // when
        TempResponseDTO.TempDTO response = tempConverter.toTempDTO(temp);

        // then
        assertThat(response.getTempId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("테스트");
    }
}
