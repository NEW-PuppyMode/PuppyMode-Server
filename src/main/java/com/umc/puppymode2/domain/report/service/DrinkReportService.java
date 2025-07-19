package com.umc.puppymode2.domain.report.service;

import com.umc.puppymode2.domain.report.dto.DrinkReportResponseDTO;

public interface DrinkReportService {
    DrinkReportResponseDTO drinkReport(Long userId);
}
