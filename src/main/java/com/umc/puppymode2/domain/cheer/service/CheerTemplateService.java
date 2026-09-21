package com.umc.puppymode2.domain.cheer.service;

import com.umc.puppymode2.domain.cheer.dto.CheerTemplateListResponseDTO;

public interface CheerTemplateService {

    // 활성 응원 문구를 분류별로 조회한다.
    CheerTemplateListResponseDTO getTemplates();
}
