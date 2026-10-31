package com.medbooking.patterns;

import com.medbooking.entity.Appointment;
import com.medbooking.entity.Payment;
import com.medbooking.exception.BusinessException;
import com.medbooking.patterns.payment.*;
import com.medbooking.repository.AppointmentRepository;
import com.medbooking.repository.PaymentRepository;
import com.medbooking.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentServicePatternIntegrationTest {
    @Test
    void prepareCashUsesCashStrategyWithoutExternalGateway() {
        PaymentRepository payments = mock(PaymentRepository.class);
        AppointmentRepository appointments = mock(AppointmentRepository.class);
        Payment payment = new Payment();
        Appointment appointment = new Appointment();
        appointment.setId(12);
        payment.setAppointment(appointment);
        payment.setStatus(Payment.Status.pending);
        payment.setAmount(new BigDecimal("500000"));
        payment.setPaymentMethod(Payment.PaymentMethod.cash);
        when(payments.findByAppointmentId(12)).thenReturn(Optional.of(payment));
        PaymentPreparationStrategyRegistry registry = new PaymentPreparationStrategyRegistry(
                List.of(new CashPaymentPreparationStrategy()));
        PaymentServiceImpl service = new PaymentServiceImpl(payments, appointments, registry);
        Map<String, Object> response = service.preparePayment(12, "cash");
        assertEquals("cash", response.get("paymentMethod"));
        verify(payments, never()).save(any());
        assertThrows(BusinessException.class, () -> service.preparePayment(12, "momo"));
    }
}
