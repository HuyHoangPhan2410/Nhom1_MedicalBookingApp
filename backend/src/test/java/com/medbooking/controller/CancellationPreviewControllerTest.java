package com.medbooking.controller;

import com.medbooking.dto.response.CancellationPreviewResponse;
import com.medbooking.dto.response.AppointmentResponse;
import com.medbooking.exception.BusinessException;
import com.medbooking.exception.GlobalExceptionHandler;
import com.medbooking.patterns.appointment.AppointmentOverviewFacade;
import com.medbooking.service.AppointmentService;
import com.medbooking.patterns.cancellation.CancellationPreviewService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CancellationPreviewControllerTest {
    @Test void getPreviewSerializesEligibilityAndQuoteWithoutCancelling() throws Exception {
        AppointmentService service = mock(AppointmentService.class);
        LocalDateTime now = LocalDateTime.of(2026, 11, 12, 7, 0);
        when(service.previewCancellation(10)).thenReturn(new CancellationPreviewResponse(
                10, "confirmed", true, null, now.plusHours(3), now,
                new BigDecimal("500000"), new BigDecimal("340000"), List.of("base", "late", "card")));
        var mvc = MockMvcBuilders.standaloneSetup(new AppointmentController(
                service, mock(AppointmentOverviewFacade.class)),
                new CancellationPreviewController(new CancellationPreviewService(service))).build();
        mvc.perform(get("/api/appointments/10/cancellation-preview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.appointmentId").value(10))
                .andExpect(jsonPath("$.data.allowed").value(true))
                .andExpect(jsonPath("$.data.cancellable").value(true))
                .andExpect(jsonPath("$.data.refundAmount").value(340000))
                .andExpect(jsonPath("$.data.refundNotes.length()").value(3));
        verify(service).previewCancellation(10);
        verifyNoMoreInteractions(service);
    }

    @Test void appointmentJsonExposesStateCapabilitiesForFrontend() throws Exception {
        AppointmentService service = mock(AppointmentService.class);
        AppointmentResponse response = new AppointmentResponse();
        response.setId(10);
        response.setStatus("pending");
        response.setCanCancel(true);
        response.setCanReschedule(true);
        when(service.getAppointmentById(10)).thenReturn(response);
        var mvc = MockMvcBuilders.standaloneSetup(new AppointmentController(
                service, mock(AppointmentOverviewFacade.class)),
                new CancellationPreviewController(new CancellationPreviewService(service))).build();
        mvc.perform(get("/api/appointments/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.canCancel").value(true))
                .andExpect(jsonPath("$.data.canReschedule").value(true));
    }

    @Test void blockedPreviewIsAValidReadOnlyResponse() throws Exception {
        AppointmentService service = mock(AppointmentService.class);
        LocalDateTime now = LocalDateTime.of(2026, 11, 12, 7, 0);
        when(service.previewCancellation(10)).thenReturn(new CancellationPreviewResponse(
                10, "completed", false, "Lịch hẹn đã hoàn thành", now.minusHours(1), now,
                new BigDecimal("500000"), BigDecimal.ZERO, List.of("Không đủ điều kiện hủy")));
        var mvc = MockMvcBuilders.standaloneSetup(new AppointmentController(
                service, mock(AppointmentOverviewFacade.class)),
                new CancellationPreviewController(new CancellationPreviewService(service))).build();
        mvc.perform(get("/api/appointments/10/cancellation-preview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.allowed").value(false))
                .andExpect(jsonPath("$.data.cancellable").value(false))
                .andExpect(jsonPath("$.data.rejectReason").value("Lịch hẹn đã hoàn thành"))
                .andExpect(jsonPath("$.data.reason").value("Lịch hẹn đã hoàn thành"))
                .andExpect(jsonPath("$.data.refundAmount").value(0));
        verify(service).previewCancellation(10);
        verifyNoMoreInteractions(service);
    }

    @Test void cancellationViolationIsHttp400() throws Exception {
        AppointmentService service = mock(AppointmentService.class);
        when(service.cancelAppointment(10)).thenThrow(new BusinessException(400, "Cần báo trước 120 phút"));
        var mvc = MockMvcBuilders.standaloneSetup(new AppointmentController(
                service, mock(AppointmentOverviewFacade.class)),
                new CancellationPreviewController(new CancellationPreviewService(service)))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(put("/api/appointments/10/cancel"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cần báo trước 120 phút"));
    }

    @Test void missingPreviewIsHttp404() throws Exception {
        AppointmentService service = mock(AppointmentService.class);
        when(service.previewCancellation(404)).thenThrow(new BusinessException(404, "Lịch hẹn không tồn tại"));
        var mvc = MockMvcBuilders.standaloneSetup(new AppointmentController(
                service, mock(AppointmentOverviewFacade.class)),
                new CancellationPreviewController(new CancellationPreviewService(service)))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(get("/api/appointments/404/cancellation-preview"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Lịch hẹn không tồn tại"));
    }
}
