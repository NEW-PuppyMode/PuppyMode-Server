package com.umc.puppymode2.domain.cheer.service;

import com.umc.puppymode2.domain.cheer.converter.CheerConverter;
import com.umc.puppymode2.domain.cheer.dto.CheerTemplateListResponseDTO;
import com.umc.puppymode2.domain.cheer.repository.CheerTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CheerTemplateServiceImpl implements CheerTemplateService {

    private final CheerTemplateRepository cheerTemplateRepository;
    private final CheerConverter converter;

    @Override
    public CheerTemplateListResponseDTO getTemplates() {
        return converter.toTemplateListDto(
                cheerTemplateRepository.findAllByActiveTrueOrderByDisplayOrderAscCheerTemplateIdAsc());
    }
}
