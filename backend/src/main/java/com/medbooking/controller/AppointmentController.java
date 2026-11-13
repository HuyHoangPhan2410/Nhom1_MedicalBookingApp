package com.medbooking.controller;

import com.medbooking.dto.request.BookAppointmentRequest;
import com.medbooking.dto.response.ApiResponse;
import com.medbooking.dto.response.AppointmentResponse;
import com.medbooking.service.AppointmentService;
import com.medbooking.patterns.appointment.AppointmentOverviewFacade;
import com.medbooking.dto.response.AppointmentOverviewResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller xử lý các yêu cầu liên quan tới lịch hẹn khám bệnh.
 */
@Tag(name = "Appointments", description = "Appointment booking, lookup, cancellation and rescheduling")
@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final AppointmentOverviewFacade overviewFacade;

    public AppointmentController(AppointmentService appointmentService, AppointmentOverviewFacade overviewFacade) {
        this.appointmentService = appointmentService;
        this.overviewFacade = overviewFacade;
    }

    @Operation(
            summary = "Book an appointment",
            description = "Books an available schedule through the transactional booking service.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Appointment booked successfully",
                    content = @Content(schema = @Schema(implementation = AppointmentResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Invalid request or business rule violation"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Patient, doctor or schedule not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500", description = "Internal server error")
    })
    @PostMapping
    public ApiResponse<AppointmentResponse> bookAppointment(@RequestBody BookAppointmentRequest request) {
        AppointmentResponse response = appointmentService.bookAppointment(request);
        return ApiResponse.success("Đặt lịch khám thành công", response);
    }

    @GetMapping("/{id}")
    public ApiResponse<AppointmentResponse> getAppointmentById(@PathVariable Integer id) {
        AppointmentResponse response = appointmentService.getAppointmentById(id);
        return ApiResponse.success(response);
    }

    // Facade combines appointment and payment details for a single screen.
    @GetMapping("/{id}/overview")
    public ApiResponse<AppointmentOverviewResponse> getOverview(@PathVariable Integer id) {
        return ApiResponse.success(overviewFacade.getOverview(id));
    }

    @GetMapping("/doctor/{doctorId}")
    public ApiResponse<List<AppointmentResponse>> getAppointmentsByDoctorId(@PathVariable Integer doctorId) {
        List<AppointmentResponse> response = appointmentService.getAppointmentsByDoctorId(doctorId);
        return ApiResponse.success(response);
    }

    @PutMapping("/{id}/cancel")
    public ApiResponse<AppointmentResponse> cancelAppointment(@PathVariable Integer id) {
        AppointmentResponse response = appointmentService.cancelAppointment(id);
        return ApiResponse.success("Đã hủy lịch khám thành công", response);
    }

    @PutMapping("/{id}/reschedule")
    public ApiResponse<AppointmentResponse> rescheduleAppointment(
            @PathVariable Integer id,
            @RequestParam Integer newScheduleId) {
        AppointmentResponse response = appointmentService.rescheduleAppointment(id, newScheduleId);
        return ApiResponse.success("Đổi lịch khám thành công", response);
    }
}
