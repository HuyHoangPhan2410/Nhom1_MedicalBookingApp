package com.medbooking.patterns.cancellation.state;

import com.medbooking.entity.Appointment;

public final class CompletedState extends AbstractAppointmentState {
    @Override public Appointment.Status status() { return Appointment.Status.completed; }
    @Override public String cancelBlockedReason() { return "Lịch hẹn đã hoàn thành, không thể hủy"; }
}
