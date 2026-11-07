package com.medbooking.patterns.cancellation.state;

import com.medbooking.entity.Appointment;

/**
 * Context của State pattern: giữ trạng thái hiện tại và ủy quyền mọi hành động cho state đó.
 * Chỉ thao tác trên đối tượng trong bộ nhớ; việc ghi CSDL vẫn do stored procedure đảm nhiệm.
 */
public class AppointmentLifecycle {
    private AppointmentState state;

    private AppointmentLifecycle(AppointmentState state) {
        this.state = state;
    }

    public static AppointmentLifecycle of(Appointment appointment) {
        return new AppointmentLifecycle(stateFor(appointment.getStatus()));
    }

    static AppointmentState stateFor(Appointment.Status status) {
        return switch (status) {
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
