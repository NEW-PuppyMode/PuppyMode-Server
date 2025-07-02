package com.umc.puppymode2.domain.temp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.umc.puppymode2.domain.temp.converter.TempConverter;
import com.umc.puppymode2.domain.temp.dto.TempRequestDTO;
import com.umc.puppymode2.domain.temp.entity.Temp;
import com.umc.puppymode2.domain.temp.repository.TempRepository;
import com.umc.puppymode2.global.apiPayload.code.status.ErrorStatus;
import com.umc.puppymode2.global.exception.handler.TempHandler;

@Service
@Transactional
@RequiredArgsConstructor
public class TempCommandServiceImpl implements TempCommandService {

    private final TempRepository tempRepository;

    @Override
    public Long createTemp(TempRequestDTO.CreateTempDTO request) {
        Temp temp = TempConverter.toTemp(request);
        Temp savedTemp = tempRepository.save(temp);
        return savedTemp.getId();
    }

    @Override
    public void updateTemp(Long tempId, TempRequestDTO.UpdateTempDTO request) {
        Temp temp = tempRepository.findById(tempId)
                .orElseThrow(() -> new TempHandler(ErrorStatus.TEMP_NOT_FOUND));

        temp.updateDescription(request.getDescription());
        temp.updateStatus(request.getStatus());
    }

    @Override
    public void deleteTemp(Long tempId) {
        Temp temp = tempRepository.findById(tempId)
                .orElseThrow(() -> new TempHandler(ErrorStatus.TEMP_NOT_FOUND));

        tempRepository.delete(temp);
    }
}
