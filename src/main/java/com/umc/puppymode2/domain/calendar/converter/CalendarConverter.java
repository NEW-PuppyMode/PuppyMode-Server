package com.umc.puppymode2.domain.calendar.converter;

import com.umc.puppymode2.domain.calendar.dto.CalendarResponseDTO;
import com.umc.puppymode2.domain.drinkhistory.entity.DrinkHistory;

import java.util.List;
import java.util.stream.Collectors;

public class CalendarConverter {
    public static CalendarResponseDTO toCalendarResponseDTO(DrinkHistory entity) {
        return CalendarResponseDTO.builder()
                .date(entity.getDrinkDate())
                .isDrink(entity.getIsDrink())
                .build();
    }

    public static List<CalendarResponseDTO> toCalendarResponseDTOList(List<DrinkHistory> entities) {
        return entities.stream()
                .map(CalendarConverter::toCalendarResponseDTO)
                .collect(Collectors.toList());
    }


}
