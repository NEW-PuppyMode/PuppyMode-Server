package com.umc.puppymode2.domain.report.service;

import com.umc.puppymode2.domain.report.dto.DrinkReportResponseDTO;
import java.time.YearMonth;

public interface DrinkReportService {
    DrinkReportResponseDTO drinkReport(Long userId, YearMonth targetMonth);
}
