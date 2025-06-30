package com.umc.puppymode2.domain.temp.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.umc.puppymode2.domain.temp.entity.enums.TempStatus;

import static org.assertj.core.api.Assertions.*;

class TempTest {

    @Test
    @DisplayName("Temp 엔티티 생성 테스트")
    void createTempEntity() {
        // given
        String testName = "테스트";
        TempStatus status = TempStatus.ACTIVE;

        // when
        Temp temp = Temp.builder()
                .name(testName)
                .status(status)
                .build();

        // then
        assertThat(temp.getName()).isEqualTo(testName);
        assertThat(temp.getStatus()).isEqualTo(status);
    }

    @Test
    @DisplayName("Temp 엔티티 상태 변경 테스트")
    void changeTempStatus() {
        // given
        Temp temp = Temp.builder()
                .name("테스트")
                .status(TempStatus.ACTIVE)
                .build();

        // when
        temp.updateStatus(TempStatus.INACTIVE);

        // then
        assertThat(temp.getStatus()).isEqualTo(TempStatus.INACTIVE);
    }
}
