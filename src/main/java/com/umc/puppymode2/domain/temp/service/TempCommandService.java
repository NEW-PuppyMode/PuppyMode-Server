package com.umc.puppymode2.domain.temp.service;

import com.umc.puppymode2.domain.temp.dto.TempRequestDTO;

public interface TempCommandService {
    Long createTemp(TempRequestDTO.CreateTempDTO request);
    void updateTemp(Long tempId, TempRequestDTO.UpdateTempDTO request);
    void deleteTemp(Long tempId);
}
