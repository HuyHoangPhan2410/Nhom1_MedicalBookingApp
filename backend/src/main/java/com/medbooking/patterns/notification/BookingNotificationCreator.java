package com.medbooking.patterns.notification;

import com.medbooking.event.AppointmentBookedEvent;

/** Factory Method: subclasses decide which BookingNotification to create. */
public abstract class BookingNotificationCreator {
    protected abstract BookingNotification createNotification();

    public final void notifyBooking(AppointmentBookedEvent event) {
        createNotification().send(event);
    }
}
