package com.medbooking.patterns.cancellation;

import com.medbooking.dto.response.CancellationPreviewResponse;
import com.medbooking.service.AppointmentService;
import org.springframework.stereotype.Service;

/** Read-only preview delegates to the same clock and rules used for actual cancellation. */
@Service
public class CancellationPreviewService {
    private final AppointmentService appointmentService;

    public CancellationPreviewService(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    public CancellationPreviewResponse preview(Integer appointmentId) {
        return appointmentService.previewCancellation(appointmentId);
    }
}
