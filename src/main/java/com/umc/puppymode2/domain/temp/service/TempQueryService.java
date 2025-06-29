package com.umc.puppymode2.domain.temp.service;

import com.umc.puppymode2.domain.temp.entity.Temp;
import com.umc.puppymode2.domain.temp.entity.enums.TempStatus;

import java.util.List;

public interface TempQueryService {
    Temp findTempById(Long tempId);
    List<Temp> findTempsByStatus(TempStatus status);
    List<Temp> findAllTemps();
    List<Temp> findTempsByName(String name);
}
