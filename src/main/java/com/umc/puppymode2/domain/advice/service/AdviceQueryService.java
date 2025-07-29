package com.umc.puppymode2.domain.advice.service;

import com.umc.puppymode2.domain.advice.dto.AdviceResponseDTO;

public interface AdviceQueryService {
    AdviceResponseDTO getAdvice(Long userId);
}
