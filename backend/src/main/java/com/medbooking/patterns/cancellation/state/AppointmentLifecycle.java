package com.medbooking.patterns.cancellation.state;

import com.medbooking.entity.Appointment;
import java.util.Objects;

/** Context delegates to State. Changing this context does not persist the entity. */
public final class AppointmentLifecycle {
    private AppointmentState state;

    private AppointmentLifecycle(AppointmentState state) { this.state = state; }

    public static AppointmentLifecycle of(Appointment appointment) {
        return new AppointmentLifecycle(stateFor(appointment.getStatus()));
    }

    public static AppointmentState stateFor(Appointment.Status status) {
        return switch (Objects.requireNonNull(status, "Appointment status is required")) {
            case pending -> new PendingState();
            case confirmed -> new ConfirmedState();
            case cancelled -> new CancelledState();
            case completed -> new CompletedState();
        };
    }

    public Appointment.Status status() { return state.status(); }
    public boolean canCancel() { return state.canCancel(); }
    public boolean canReschedule() { return state.canReschedule(); }
    public String cancelBlockedReason() { return state.cancelBlockedReason(); }
    public void confirm() { state = state.confirm(); }
    public void cancel() { state = state.cancel(); }
    public void complete() { state = state.complete(); }
}
