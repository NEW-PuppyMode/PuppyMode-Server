package com.umc.puppymode2.domain.temp.service;

import com.umc.puppymode2.domain.temp.dto.TempRequestDTO;
import com.umc.puppymode2.domain.temp.entity.Temp;
import com.umc.puppymode2.domain.temp.entity.enums.TempStatus;
import com.umc.puppymode2.domain.temp.repository.TempRepository;
import com.umc.puppymode2.global.exception.handler.TempHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class TempCommandServiceTest {

    @Mock
    private TempRepository tempRepository;

    @InjectMocks
    private TempCommandServiceImpl tempCommandService;

    @Test
    @DisplayName("Temp 생성 테스트")
    void createTemp() {
        // given
        TempRequestDTO.CreateTempDTO request = TempRequestDTO.CreateTempDTO.builder()
                .name("테스트")
                .description("설명입니다")
                .build();

        Temp temp = Temp.builder()
                .id(1L)
                .name(request.getName())
                .description(request.getDescription())
                .status(TempStatus.ACTIVE)
                .build();

        given(tempRepository.save(any(Temp.class))).willReturn(temp);

        // when
        Long savedId = tempCommandService.createTemp(request);

        // then
        assertThat(savedId).isEqualTo(1L);
        then(tempRepository).should().save(any(Temp.class));
    }

    @Test
    @DisplayName("Temp 상태 변경 테스트")
    void changeTempStatus() {
        // given
        Long tempId = 1L;
        TempStatus newStatus = TempStatus.INACTIVE;

        Temp temp = Temp.builder()
                .id(tempId)
                .name("테스트")
                .status(TempStatus.ACTIVE)
                .build();

        given(tempRepository.findById(tempId)).willReturn(Optional.of(temp));

        // when
        tempCommandService.updateTemp(tempId,
                TempRequestDTO.UpdateTempDTO.builder()
                        .description("업데이트된 설명")
                        .status(newStatus)
                        .build());

        // then
        assertThat(temp.getStatus()).isEqualTo(newStatus);
        assertThat(temp.getDescription()).isEqualTo("업데이트된 설명");
        then(tempRepository).should().findById(tempId);
    }

    @Test
    @DisplayName("Temp 삭제 테스트")
    void deleteTemp() {
        // given
        Long tempId = 1L;
        Temp temp = Temp.builder()
                .id(tempId)
                .name("테스트")
                .status(TempStatus.ACTIVE)
                .build();

        given(tempRepository.findById(tempId)).willReturn(Optional.of(temp));
        willDoNothing().given(tempRepository).delete(temp);

        // when
        tempCommandService.deleteTemp(tempId);

        // then
        then(tempRepository).should().findById(tempId);
        then(tempRepository).should().delete(temp);
    }

    @Test
    @DisplayName("존재하지 않는 Temp 수정 시 예외 발생 테스트")
    void updateNonExistingTempThrows() {
        // given
        Long tempId = 999L;
        given(tempRepository.findById(tempId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                tempCommandService.updateTemp(tempId,
                        TempRequestDTO.UpdateTempDTO.builder()
                                .description("내용")
                                .status(TempStatus.ACTIVE)
                                .build()))
                .isInstanceOf(TempHandler.class);
    }

    @Test
    @DisplayName("존재하지 않는 Temp 삭제 시 예외 발생 테스트")
    void deleteNonExistingTempThrows() {
        // given
        Long tempId = 999L;
        given(tempRepository.findById(tempId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> tempCommandService.deleteTemp(tempId))
                .isInstanceOf(TempHandler.class);
    }
}
