package com.umc.puppymode2.domain.calendar.controller;

import com.umc.puppymode2.domain.calendar.dto.CalendarResponseDTO;
import com.umc.puppymode2.domain.calendar.service.CalendarQueryService;
import com.umc.puppymode2.global.apiPayload.ApiResponse;
import com.umc.puppymode2.global.apiPayload.code.status.SuccessStatus;
import com.umc.puppymode2.global.auth.context.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "calendar-controller", description = "캘린더 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/calendar")
public class CalendarController {
    private final CalendarQueryService calendarQueryService;
    private final UserContext userContext;

    @Operation(summary = "캘린더 조회", description = "캘린더를 조회하는 API 입니다.")
    @GetMapping
    public ApiResponse<List<CalendarResponseDTO>> getCalendar(@RequestParam int year,
                                                              @RequestParam int month) {
        Long userId = userContext.getCurrentUserId();
        List<CalendarResponseDTO> drinkStatus = calendarQueryService.getCalendar(userId, year, month);
        return ApiResponse.onSuccess(drinkStatus, SuccessStatus.CALENDAR_GET_SUCCESS.getCode(), SuccessStatus.CALENDAR_GET_SUCCESS.getMessage());
    }

}
