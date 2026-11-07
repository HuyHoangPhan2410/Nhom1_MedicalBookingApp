package com.medbooking.patterns.cancellation.state;

import com.medbooking.entity.Appointment;

/** Đã xác nhận: được hủy, đổi lịch hoặc hoàn thành sau buổi khám. */
public class ConfirmedState extends AbstractAppointmentState {
    @Override public Appointment.Status status() { return Appointment.Status.confirmed; }
    @Override public boolean canCancel() { return true; }
    @Override public boolean canReschedule() { return true; }
    @Override public AppointmentState cancel() { return new CancelledState(); }
    @Override public AppointmentState complete() { return new CompletedState(); }
}
