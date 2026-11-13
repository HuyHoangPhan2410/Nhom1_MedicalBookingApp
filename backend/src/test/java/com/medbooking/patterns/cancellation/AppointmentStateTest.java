package com.medbooking.patterns.cancellation;

import com.medbooking.entity.Appointment;
import com.medbooking.exception.BusinessException;
import com.medbooking.patterns.cancellation.state.AppointmentLifecycle;
import org.junit.jupiter.api.Test;

import static com.medbooking.patterns.cancellation.CancellationTestSupport.NOW;
import static com.medbooking.patterns.cancellation.CancellationTestSupport.appointment;
import static org.junit.jupiter.api.Assertions.*;

class AppointmentStateTest {
    private AppointmentLifecycle lifecycleOf(Appointment.Status status) {
        return AppointmentLifecycle.of(appointment(status, NOW.plusDays(2)));
    }

    @Test
    void pendingCanBeConfirmedThenCompleted() {
        AppointmentLifecycle lifecycle = lifecycleOf(Appointment.Status.pending);
        lifecycle.confirm();
        assertEquals(Appointment.Status.confirmed, lifecycle.status());
        lifecycle.complete();
        assertEquals(Appointment.Status.completed, lifecycle.status());
    }

    @Test
    void pendingAndConfirmedAllowCancelAndReschedule() {
        for (Appointment.Status status : new Appointment.Status[]{Appointment.Status.pending, Appointment.Status.confirmed}) {
            AppointmentLifecycle lifecycle = lifecycleOf(status);
            assertTrue(lifecycle.canCancel());
            assertTrue(lifecycle.canReschedule());
            assertNull(lifecycle.cancelBlockedReason());
        }
    }

    @Test
    void cancelledAndCompletedAreTerminalStates() {
        for (Appointment.Status status : new Appointment.Status[]{Appointment.Status.cancelled, Appointment.Status.completed}) {
            AppointmentLifecycle lifecycle = lifecycleOf(status);
            assertFalse(lifecycle.canCancel());
            assertFalse(lifecycle.canReschedule());
            assertNotNull(lifecycle.cancelBlockedReason());
            assertThrows(BusinessException.class, lifecycle::cancel);
            assertThrows(BusinessException.class, lifecycle::confirm);
            assertThrows(BusinessException.class, lifecycle::complete);
        }
    }

    @Test
    void pendingCannotSkipConfirmationToCompleted() {
        assertThrows(BusinessException.class, () -> lifecycleOf(Appointment.Status.pending).complete());
    }

    @Test
    void cancelMovesToCancelledAndBlocksSecondCancel() {
        AppointmentLifecycle lifecycle = lifecycleOf(Appointment.Status.confirmed);
        lifecycle.cancel();
        assertEquals(Appointment.Status.cancelled, lifecycle.status());
        assertThrows(BusinessException.class, lifecycle::cancel);
    }
}
