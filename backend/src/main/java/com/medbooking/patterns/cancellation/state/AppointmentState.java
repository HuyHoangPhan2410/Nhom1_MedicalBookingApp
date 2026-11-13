package com.medbooking.patterns.cancellation.state;

import com.medbooking.entity.Appointment;

/** State: each appointment status owns its allowed actions and transitions. */
public interface AppointmentState {
    Appointment.Status status();
    boolean canCancel();
    boolean canReschedule();
    String cancelBlockedReason();
    AppointmentState confirm();
    AppointmentState cancel();
    AppointmentState complete();
}
