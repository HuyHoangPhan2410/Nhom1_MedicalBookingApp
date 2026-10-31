package com.medbooking.patterns;

import com.medbooking.dto.response.AppointmentOverviewResponse;
import com.medbooking.dto.response.AppointmentResponse;
import com.medbooking.dto.response.PaymentResponse;
import com.medbooking.patterns.appointment.AppointmentOverviewFacade;
import com.medbooking.service.AppointmentService;
import com.medbooking.service.PaymentService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AppointmentOverviewFacadeTest {
    @Test
    void facadeCombinesAppointmentAndPaymentIntoOneView() {
        AppointmentService appointmentService = mock(AppointmentService.class);
        PaymentService paymentService = mock(PaymentService.class);
        AppointmentResponse appointment = new AppointmentResponse();
        appointment.setId(15);
        PaymentResponse payment = new PaymentResponse();
        payment.setAppointmentId(15);
        when(appointmentService.getAppointmentById(15)).thenReturn(appointment);
        when(paymentService.getPaymentByAppointmentId(15)).thenReturn(payment);

        AppointmentOverviewResponse overview = new AppointmentOverviewFacade(
                appointmentService, paymentService).getOverview(15);
        assertSame(appointment, overview.appointment());
        assertSame(payment, overview.payment());
        verify(appointmentService).getAppointmentById(15);
        verify(paymentService).getPaymentByAppointmentId(15);
    }
}
