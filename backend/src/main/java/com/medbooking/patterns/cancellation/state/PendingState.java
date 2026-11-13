package com.medbooking.patterns.cancellation.state;

import com.medbooking.entity.Appointment;

public final class PendingState extends AbstractAppointmentState {
    @Override public Appointment.Status status() { return Appointment.Status.pending; }
    @Override public boolean canCancel() { return true; }
    @Override public boolean canReschedule() { return true; }
    @Override public String cancelBlockedReason() { return null; }
    @Override public AppointmentState confirm() { return new ConfirmedState(); }
    @Override public AppointmentState cancel() { return new CancelledState(); }
}
