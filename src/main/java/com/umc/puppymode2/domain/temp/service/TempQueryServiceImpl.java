package com.umc.puppymode2.domain.temp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.umc.puppymode2.domain.temp.entity.Temp;
import com.umc.puppymode2.domain.temp.entity.enums.TempStatus;
import com.umc.puppymode2.domain.temp.repository.TempRepository;
import com.umc.puppymode2.global.apiPayload.code.status.ErrorStatus;
import com.umc.puppymode2.global.exception.handler.TempHandler;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TempQueryServiceImpl implements TempQueryService {

    private final TempRepository tempRepository;

    @Override
    public Temp findTempById(Long tempId) {
        return tempRepository.findById(tempId)
                .orElseThrow(() -> new TempHandler(ErrorStatus.TEMP_NOT_FOUND));
    }

    @Override
    public List<Temp> findTempsByStatus(TempStatus status) {
        return tempRepository.findByStatus(status);
    }

    @Override
    public List<Temp> findAllTemps() {
        return tempRepository.findAll();
    }

    @Override
    public List<Temp> findTempsByName(String name) {
        return tempRepository.findByNameContaining(name);
    }
}
