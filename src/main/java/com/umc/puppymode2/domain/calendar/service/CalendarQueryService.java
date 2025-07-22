package com.umc.puppymode2.domain.calendar.service;

import com.umc.puppymode2.domain.calendar.dto.CalendarResponseDTO;

import java.util.List;

public interface CalendarQueryService {
    List<CalendarResponseDTO> getCalendar(Long userId, int year, int month);
}
