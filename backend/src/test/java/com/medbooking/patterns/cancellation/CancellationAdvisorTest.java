package com.medbooking.patterns.cancellation;

import com.medbooking.dto.response.CancellationPreviewResponse;
import com.medbooking.entity.Appointment;
import com.medbooking.entity.Payment;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.medbooking.patterns.cancellation.CancellationTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

/** Kiểm thử tích hợp ba pattern State + Chain + Decorator qua CancellationAdvisor. */
class CancellationAdvisorTest {
    private final CancellationAdvisor advisor = new CancellationAdvisor();

    @Test
    void allowedPreviewReturnsRefundAndNotes() {
        Appointment appointment = appointment(Appointment.Status.confirmed, NOW.plusHours(10));
        Payment card = payment(Payment.PaymentMethod.card, Payment.Status.success, "500000");

        CancellationPreviewResponse response = advisor.advise(appointment, card, NOW);

        assertTrue(response.cancellable());
        assertNull(response.rejectReason());
        assertEquals(0, new BigDecimal("340000").compareTo(response.refundAmount()));
        assertEquals(3, response.refundNotes().size());
        assertEquals("confirmed", response.status());
    }

    @Test
    void rejectedPreviewHasReasonAndZeroRefund() {
        Appointment appointment = appointment(Appointment.Status.completed, NOW.plusHours(10));
        Payment cash = payment(Payment.PaymentMethod.cash, Payment.Status.success, "500000");

        CancellationPreviewResponse response = advisor.advise(appointment, cash, NOW);

        assertFalse(response.cancellable());
        assertNotNull(response.rejectReason());
        assertEquals(0, BigDecimal.ZERO.compareTo(response.refundAmount()));
        assertTrue(response.refundNotes().isEmpty());
    }

    @Test
    void unpaidAppointmentCanBeCancelledWithZeroRefund() {
        Appointment appointment = appointment(Appointment.Status.pending, NOW.plusDays(3));

        CancellationPreviewResponse response = advisor.advise(appointment, null, NOW);

        assertTrue(response.cancellable());
        assertEquals(0, BigDecimal.ZERO.compareTo(response.refundAmount()));
    }
}
