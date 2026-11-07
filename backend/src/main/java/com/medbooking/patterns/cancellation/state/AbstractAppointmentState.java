package com.medbooking.patterns.cancellation.state;

import com.medbooking.exception.BusinessException;

/** Mặc định mọi chuyển trạng thái đều bị từ chối; lớp con chỉ mở những chuyển hợp lệ. */
abstract class AbstractAppointmentState implements AppointmentState {

    @Override
    public boolean canCancel() {
        return false;
    }

    @Override
    public boolean canReschedule() {
        return false;
    }

    @Override
    public String cancelBlockedReason() {
        return canCancel() ? null : "Lịch hẹn ở trạng thái '" + status().name() + "' không thể hủy.";
    }

    @Override
    public AppointmentState confirm() {
        throw illegal("xác nhận");
    }

    @Override
    public AppointmentState cancel() {
        throw illegal("hủy");
    }

    @Override
    public AppointmentState complete() {
        throw illegal("hoàn thành");
    }

    private BusinessException illegal(String action) {
        return new BusinessException(400,
                "Không thể " + action + " lịch hẹn đang ở trạng thái '" + status().name() + "'.");
    }
}
