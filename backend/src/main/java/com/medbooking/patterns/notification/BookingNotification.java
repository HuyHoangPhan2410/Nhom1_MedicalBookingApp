package com.medbooking.patterns.notification;

import com.medbooking.event.AppointmentBookedEvent;

@FunctionalInterface
public interface BookingNotification {
    void send(AppointmentBookedEvent event);
}
