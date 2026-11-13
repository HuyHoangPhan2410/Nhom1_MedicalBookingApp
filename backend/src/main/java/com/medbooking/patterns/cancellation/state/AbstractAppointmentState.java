package com.medbooking.patterns.cancellation.state;

import com.medbooking.exception.BusinessException;

abstract class AbstractAppointmentState implements AppointmentState {
    @Override public boolean canCancel() { return false; }
    @Override public boolean canReschedule() { return false; }
    @Override public String cancelBlockedReason() {
        return "Không thể hủy lịch hẹn ở trạng thái " + status();
    }
    @Override public AppointmentState confirm() { throw blocked("xác nhận"); }
    @Override public AppointmentState cancel() { throw blocked("hủy"); }
    @Override public AppointmentState complete() { throw blocked("hoàn thành"); }

    private BusinessException blocked(String action) {
        return new BusinessException(400, "Không thể " + action + " lịch hẹn ở trạng thái " + status());
    }
}
