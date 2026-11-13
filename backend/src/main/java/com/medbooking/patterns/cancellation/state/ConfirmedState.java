package com.medbooking.patterns.cancellation.state;

import com.medbooking.entity.Appointment;

public final class ConfirmedState extends AbstractAppointmentState {
    @Override public Appointment.Status status() { return Appointment.Status.confirmed; }
    @Override public boolean canCancel() { return true; }
    @Override public boolean canReschedule() { return true; }
    @Override public String cancelBlockedReason() { return null; }
    @Override public AppointmentState cancel() { return new CancelledState(); }
    @Override public AppointmentState complete() { return new CompletedState(); }
}
