package com.medbooking.service;

import com.medbooking.entity.*;
import com.medbooking.exception.BusinessException;
import com.medbooking.patterns.cancellation.CancellationAdvisor;
import com.medbooking.repository.*;
import com.medbooking.service.impl.AppointmentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import java.math.BigDecimal;
import java.time.*;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Service + real patterns, repositories mocked; not a live MySQL integration test. */
@ExtendWith(MockitoExtension.class)
class CancellationServiceTest {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Instant NOW = Instant.parse("2026-11-12T00:00:00Z");
    @Mock AppointmentRepository appointments;
    @Mock PaymentRepository payments;
    @Mock ApplicationEventPublisher publisher;
    private AppointmentServiceImpl service;
    private Appointment appointment;
    private Payment payment;

    @BeforeEach void setUp() {
        service = serviceAt(NOW);
        appointment = new Appointment();
        appointment.setId(10);
        appointment.setStatus(Appointment.Status.confirmed);
        Patient patient = new Patient();
        patient.setUserId(1);
        patient.setFullName("Bệnh nhân");
        appointment.setPatient(patient);
        Doctor doctor = new Doctor();
        doctor.setUserId(2);
        doctor.setFullName("Bác sĩ");
        Specialty specialty = new Specialty();
        specialty.setName("Nội khoa");
        doctor.setSpecialty(specialty);
        appointment.setDoctor(doctor);
        Schedule schedule = new Schedule();
        schedule.setId(3);
        schedule.setWorkDate(LocalDate.of(2026, 11, 12));
        schedule.setStartTime(LocalTime.of(10, 0));
        schedule.setEndTime(LocalTime.of(11, 0));
        appointment.setSchedule(schedule);
        payment = new Payment();
        payment.setAppointment(appointment);
        payment.setStatus(Payment.Status.success);
        payment.setPaymentMethod(Payment.PaymentMethod.card);
        payment.setAmount(new BigDecimal("500000"));
    }

    @Test void previewIsReadOnlyAndUsesConfiguredLocalTime() {
        when(appointments.findById(10)).thenReturn(Optional.of(appointment));
        when(payments.findByAppointmentId(10)).thenReturn(Optional.of(payment));
        var preview = service.previewCancellation(10);
        assertTrue(preview.allowed());
        assertEquals(new BigDecimal("340000"), preview.refundAmount());
        assertEquals(LocalDateTime.of(2026, 11, 12, 7, 0), preview.evaluatedAt());
        assertEquals(Appointment.Status.confirmed, appointment.getStatus());
        assertEquals(Payment.Status.success, payment.getStatus());
        verify(appointments).findById(10);
        verify(payments).findByAppointmentId(10);
        verifyNoMoreInteractions(appointments, payments);
        verifyNoInteractions(publisher);
    }

    @Test void cancellationLocksAndPreservesSuccessfulChargeWithoutClaimingRefund() {
        stubLockedAppointment();
        when(payments.findByAppointmentIdForUpdate(10)).thenReturn(Optional.of(payment));
        var response = service.cancelAppointment(10);
        assertEquals("cancelled", response.getStatus());
        assertFalse(response.isCanCancel());
        assertFalse(response.isCanReschedule());
        assertEquals(Payment.Status.success, payment.getStatus());
        assertEquals(new BigDecimal("500000"), payment.getAmount());
        var order = inOrder(appointments, payments);
        order.verify(appointments).findByIdForUpdate(10);
        order.verify(payments).findByAppointmentIdForUpdate(10);
        order.verify(appointments).save(appointment);
        verifyNoMoreInteractions(appointments, payments);
        verifyNoInteractions(publisher);
    }

    @Test void cancellationInvalidatesOnlyUnpaidInvoice() {
        stubLockedAppointment();
        payment.setStatus(Payment.Status.pending);
        when(payments.findByAppointmentIdForUpdate(10)).thenReturn(Optional.of(payment));
        service.cancelAppointment(10);
        assertEquals(Payment.Status.failed, payment.getStatus());
        verify(payments).save(payment);
    }

    @Test void missingPaymentDoesNotPreventCancellation() {
        stubLockedAppointment();
        assertEquals("cancelled", service.cancelAppointment(10).getStatus());
        verify(payments, never()).save(any());
    }

    @Test void staleAllowedPreviewCannotBypassTwoHourLimit() {
        when(appointments.findById(10)).thenReturn(Optional.of(appointment));
        assertTrue(service.previewCancellation(10).allowed());
        stubLockedAppointment();
        // 08:00:01 local: only 1h59m59s left until 10:00.
        AppointmentServiceImpl laterService = serviceAt(NOW.plusSeconds(3601));
        BusinessException ex = assertThrows(BusinessException.class, () -> laterService.cancelAppointment(10));
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("120 phút"));
        assertEquals(Appointment.Status.confirmed, appointment.getStatus());
        verify(appointments, never()).save(any());
        verify(payments, never()).save(any());
    }

    @Test void cancellingTwiceIsRejectedWithoutSecondWrite() {
        stubLockedAppointment();
        service.cancelAppointment(10);
        assertThrows(BusinessException.class, () -> service.cancelAppointment(10));
        verify(appointments, times(1)).save(any());
    }

    @Test void cancelledAppointmentCannotBeRescheduled() {
        appointment.setStatus(Appointment.Status.cancelled);
        when(appointments.findById(10)).thenReturn(Optional.of(appointment));
        assertThrows(BusinessException.class, () -> service.rescheduleAppointment(10, 4));
        verify(appointments, never()).spRescheduleAppointment(anyInt(), anyInt());
    }

    @Test void adminConfirmationAndCompletionFollowStateTransitions() {
        appointment.setStatus(Appointment.Status.pending);
        stubLockedAppointment();
        assertEquals("confirmed", service.updateStatus(10, "confirmed").getStatus());
        assertEquals("completed", service.updateStatus(10, "completed").getStatus());
        assertThrows(BusinessException.class, () -> service.updateStatus(10, "confirmed"));
        verify(appointments, times(2)).save(appointment);
    }

    @Test void adminCannotCompletePendingAppointment() {
        appointment.setStatus(Appointment.Status.pending);
        stubLockedAppointment();
        assertThrows(BusinessException.class, () -> service.updateStatus(10, "completed"));
        verify(appointments, never()).save(any());
    }

    @Test void adminCancellationCannotBypassRuleChain() {
        stubLockedAppointment();
        appointment.getSchedule().setStartTime(LocalTime.of(8, 0));
        assertThrows(BusinessException.class, () -> service.updateStatus(10, "cancelled"));
        verify(appointments, never()).save(any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"unknown", "pending"})
    void invalidOrBackwardStatusIsBusinessError(String status) {
        if ("pending".equals(status)) stubLockedAppointment();
        assertEquals(400, assertThrows(BusinessException.class, () -> service.updateStatus(10, status)).getStatus());
        verify(appointments, never()).save(any());
    }

    @Test void missingAppointmentReturns404InPreview() {
        assertEquals(404, assertThrows(BusinessException.class, () -> service.previewCancellation(404)).getStatus());
        verifyNoInteractions(payments);
    }

    @Test void missingAppointmentReturns404InCancel() {
        assertEquals(404, assertThrows(BusinessException.class, () -> service.cancelAppointment(404)).getStatus());
        verifyNoInteractions(payments);
    }

    private void stubLockedAppointment() {
        when(appointments.findByIdForUpdate(10)).thenReturn(Optional.of(appointment));
    }
    private AppointmentServiceImpl serviceAt(Instant instant) {
        return new AppointmentServiceImpl(appointments, publisher, payments,
                new CancellationAdvisor(), Clock.fixed(instant, ZONE));
    }
}
