package com.medbooking.patterns.cancellation.state;

import com.medbooking.entity.Appointment;

/**
 * State (Behavioral): mỗi trạng thái của lịch hẹn tự quyết định hành động nào hợp lệ
 * và chuyển sang trạng thái kế tiếp nào, thay cho chuỗi if/else theo status.
 */
public interface AppointmentState {
    Appointment.Status status();

    boolean canCancel();

    boolean canReschedule();

    /** Lý do không cho hủy; null nếu được phép hủy. */
    String cancelBlockedReason();

    AppointmentState confirm();

    AppointmentState cancel();

    AppointmentState complete();
}
