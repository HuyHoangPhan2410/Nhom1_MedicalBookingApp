package com.medbooking.patterns.cancellation.state;

import com.medbooking.entity.Appointment;

/** Đã hoàn thành: trạng thái kết thúc, không cho chuyển tiếp. */
public class CompletedState extends AbstractAppointmentState {
    @Override public Appointment.Status status() { return Appointment.Status.completed; }
    @Override public String cancelBlockedReason() { return "Lịch hẹn đã hoàn thành, không thể hủy."; }
}
