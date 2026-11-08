package com.medbooking.controller;

import com.medbooking.dto.request.CreateScheduleRequest;
import com.medbooking.dto.request.UpdateScheduleRequest;
import com.medbooking.dto.response.ApiResponse;
import com.medbooking.dto.response.ScheduleResponse;
import com.medbooking.service.ScheduleService;
import org.springframework.format.annotation.DateTimeFormat;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * REST Controller quản lý các ca làm việc của bác sĩ (Hỗ trợ đầy đủ CRUD).
 */
@Tag(name = "Schedules", description = "Doctor schedule management")
@RestController
@RequestMapping("/api/schedules")
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @GetMapping("/doctor/{doctorId}")
    public ApiResponse<List<ScheduleResponse>> getSchedulesByDoctor(
            @PathVariable Integer doctorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<ScheduleResponse> schedules = scheduleService.getSchedulesByDoctor(doctorId, date);
        return ApiResponse.success(schedules);
    }
 
    @PostMapping
    public ApiResponse<ScheduleResponse> createSchedule(@RequestBody CreateScheduleRequest request) {
        ScheduleResponse response = scheduleService.createSchedule(request);
        return ApiResponse.success("Đăng ký ca khám mới thành công", response);
    }

    @PutMapping("/{id}")
    public ApiResponse<ScheduleResponse> updateSchedule(
            @PathVariable Integer id,
            @RequestBody UpdateScheduleRequest request) {
        ScheduleResponse response = scheduleService.updateSchedule(id, request);
        return ApiResponse.success("Cập nhật ca khám thành công", response);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> cancelSchedule(
            @PathVariable Integer id,
            @RequestParam Integer doctorId) {
        scheduleService.cancelSchedule(id, doctorId);
        return ApiResponse.success("Hủy ca làm việc thành công", null);
    }
}
