package com.medbooking.patterns.cancellation.state;

import com.medbooking.entity.Appointment;

public final class CancelledState extends AbstractAppointmentState {
    @Override public Appointment.Status status() { return Appointment.Status.cancelled; }
    @Override public String cancelBlockedReason() { return "Lịch hẹn đã được hủy trước đó."; }
}
