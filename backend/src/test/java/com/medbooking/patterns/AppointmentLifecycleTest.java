package com.medbooking.patterns;

import com.medbooking.entity.Appointment;
import com.medbooking.exception.BusinessException;
import com.medbooking.patterns.cancellation.state.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;

class AppointmentLifecycleTest {
    @Test void pendingCanConfirmThenComplete() {
        Appointment appointment = new Appointment();
        AppointmentLifecycle lifecycle = AppointmentLifecycle.of(appointment);
        lifecycle.confirm();
        assertEquals(Appointment.Status.confirmed, lifecycle.status());
        lifecycle.complete();
        assertEquals(Appointment.Status.completed, lifecycle.status());
        assertFalse(lifecycle.canCancel());
        assertFalse(lifecycle.canReschedule());
        assertEquals(Appointment.Status.pending, appointment.getStatus(), "Context alone must not persist or mutate entity");
    }

    @ParameterizedTest
    @EnumSource(value = Appointment.Status.class, names = {"pending", "confirmed"})
    void activeStatesCanCancel(Appointment.Status status) {
        Appointment appointment = new Appointment();
        appointment.setStatus(status);
        AppointmentLifecycle lifecycle = AppointmentLifecycle.of(appointment);
        assertTrue(lifecycle.canCancel());
        assertTrue(lifecycle.canReschedule());
        assertNull(lifecycle.cancelBlockedReason());
        lifecycle.cancel();
        assertEquals(Appointment.Status.cancelled, lifecycle.status());
        assertEquals(400, assertThrows(BusinessException.class, lifecycle::cancel).getStatus());
    }

    @ParameterizedTest
    @EnumSource(value = Appointment.Status.class, names = {"cancelled", "completed"})
    void terminalStatesRejectEveryTransition(Appointment.Status status) {
        AppointmentState state = AppointmentLifecycle.stateFor(status);
        assertFalse(state.canCancel());
        assertFalse(state.canReschedule());
        assertNotNull(state.cancelBlockedReason());
        assertThrows(BusinessException.class, state::cancel);
        assertThrows(BusinessException.class, state::confirm);
        assertThrows(BusinessException.class, state::complete);
    }

    @Test void pendingCannotCompleteAndConfirmedCannotConfirmAgain() {
        assertThrows(BusinessException.class, new PendingState()::complete);
        assertThrows(BusinessException.class, new ConfirmedState()::confirm);
    }
}
