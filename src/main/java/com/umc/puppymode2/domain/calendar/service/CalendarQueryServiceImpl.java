package com.umc.puppymode2.domain.calendar.service;

import com.umc.puppymode2.domain.calendar.converter.CalendarConverter;
import com.umc.puppymode2.domain.calendar.dto.CalendarResponseDTO;
import com.umc.puppymode2.domain.drinkhistory.entity.DrinkHistory;
import com.umc.puppymode2.domain.drinkhistory.repository.DrinkHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class CalendarQueryServiceImpl implements CalendarQueryService {

    private final DrinkHistoryRepository drinkHistoryRepository;

    @Override
    public List<CalendarResponseDTO> getCalendar(Long userId, int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<DrinkHistory> drinkHistories = drinkHistoryRepository
                .findAllByUserIdAndDrinkDateBetween(userId, startDate, endDate);

        return CalendarConverter.toCalendarResponseDTOList(drinkHistories);
    }
}
