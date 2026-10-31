package com.medbooking.patterns.appointment;

import com.medbooking.dto.response.AppointmentOverviewResponse;
import com.medbooking.dto.response.AppointmentResponse;
import com.medbooking.dto.response.PaymentResponse;
import com.medbooking.service.AppointmentService;
import com.medbooking.service.PaymentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Facade (Structural): one operation for the appointment overview screen. */
@Service
public class AppointmentOverviewFacade {
    private final AppointmentService appointmentService;
    private final PaymentService paymentService;

    public AppointmentOverviewFacade(AppointmentService appointmentService, PaymentService paymentService) {
        this.appointmentService = appointmentService;
        this.paymentService = paymentService;
    }

    @Transactional(readOnly = true)
    public AppointmentOverviewResponse getOverview(Integer appointmentId) {
        AppointmentResponse appointment = appointmentService.getAppointmentById(appointmentId);
        PaymentResponse payment = paymentService.getPaymentByAppointmentId(appointmentId);
        return new AppointmentOverviewResponse(appointment, payment);
    }
}
