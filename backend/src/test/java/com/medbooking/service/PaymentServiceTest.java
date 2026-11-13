package com.medbooking.service;

import com.medbooking.dto.request.ConfirmPaymentRequest;
import com.medbooking.entity.Appointment;
import com.medbooking.entity.Payment;
import com.medbooking.exception.BusinessException;
import com.medbooking.repository.AppointmentRepository;
import com.medbooking.repository.PaymentRepository;
import com.medbooking.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.*;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Confirmation integrates the real State with mocked repositories, not a payment gateway. */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {
    @Mock PaymentRepository payments;
    @Mock AppointmentRepository appointments;
    private PaymentServiceImpl service;
    private Appointment appointment;
    private Payment payment;
    private ConfirmPaymentRequest request;

    @BeforeEach void setUp() {
        service = new PaymentServiceImpl(payments, appointments, null,
                Clock.fixed(Instant.parse("2026-11-12T00:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh")));
        appointment = new Appointment();
        appointment.setId(10);
        payment = new Payment();
        payment.setId(1);
        payment.setAppointment(appointment);
        payment.setAmount(new BigDecimal("300000"));
        payment.setPaymentMethod(Payment.PaymentMethod.cash);
        payment.setStatus(Payment.Status.pending);
        request = new ConfirmPaymentRequest();
        request.setAppointmentId(10);
        request.setPaymentMethod("card");
    }

    @Test void confirmationMovesPendingToConfirmedAndRecordsPaymentInSameTransaction() {
        stubLockedRecords();
        var response = service.confirmPayment(request);
        assertEquals("success", response.getStatus());
        assertEquals(10, response.getAppointmentId());
        assertEquals(Appointment.Status.confirmed, appointment.getStatus());
        assertEquals(Payment.PaymentMethod.card, payment.getPaymentMethod());
        assertEquals(LocalDateTime.of(2026, 11, 12, 7, 0), payment.getPaidAt());
        var order = inOrder(appointments, payments);
        order.verify(appointments).findByIdForUpdate(10);
        order.verify(payments).findByAppointmentIdForUpdate(10);
        order.verify(appointments).save(appointment);
        order.verify(payments).save(payment);
        verifyNoMoreInteractions(appointments, payments);
    }

    @Test void paymentCanSettleAppointmentAlreadyConfirmedByAdmin() {
        stubLockedRecords();
        appointment.setStatus(Appointment.Status.confirmed);
        assertEquals("success", service.confirmPayment(request).getStatus());
        assertEquals(Appointment.Status.confirmed, appointment.getStatus());
    }

    @Test void repeatedConfirmationDoesNotRewriteSuccessfulPayment() {
        stubLockedRecords();
        appointment.setStatus(Appointment.Status.confirmed);
        payment.setStatus(Payment.Status.success);
        LocalDateTime paidAt = LocalDateTime.of(2026, 11, 11, 12, 0);
        payment.setPaidAt(paidAt);
        assertEquals("success", service.confirmPayment(request).getStatus());
        assertEquals(Payment.PaymentMethod.cash, payment.getPaymentMethod());
        assertEquals(paidAt, payment.getPaidAt());
        verify(appointments, never()).save(any());
        verify(payments, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = Appointment.Status.class, names = {"cancelled", "completed"})
    void terminalAppointmentCannotBeRevivedByPayment(Appointment.Status status) {
        stubLockedRecords();
        appointment.setStatus(status);
        payment.setStatus(Payment.Status.success);
        assertEquals(400, assertThrows(BusinessException.class, () -> service.confirmPayment(request)).getStatus());
        assertEquals(status, appointment.getStatus());
        verify(appointments, never()).save(any());
        verify(payments, never()).save(any());
    }

    @Test void failedInvoiceCannotBeConfirmed() {
        stubLockedRecords();
        payment.setStatus(Payment.Status.failed);
        assertThrows(BusinessException.class, () -> service.confirmPayment(request));
        assertEquals(Appointment.Status.pending, appointment.getStatus());
        verify(appointments, never()).save(any());
        verify(payments, never()).save(any());
    }

    @Test void missingAppointmentIs404() {
        assertEquals(404, assertThrows(BusinessException.class, () -> service.confirmPayment(request)).getStatus());
        verifyNoInteractions(payments);
    }

    @Test void missingInvoiceIs404() {
        when(appointments.findByIdForUpdate(10)).thenReturn(Optional.of(appointment));
        assertEquals(404, assertThrows(BusinessException.class, () -> service.confirmPayment(request)).getStatus());
        verify(appointments, never()).save(any());
    }

    private void stubLockedRecords() {
        when(appointments.findByIdForUpdate(10)).thenReturn(Optional.of(appointment));
        when(payments.findByAppointmentIdForUpdate(10)).thenReturn(Optional.of(payment));
    }
}
