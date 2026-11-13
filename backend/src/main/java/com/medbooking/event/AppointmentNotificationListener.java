package com.medbooking.event;

import com.medbooking.patterns.notification.BookingNotificationCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Observer: reacts to booking event after a successful DB transaction. */
@Component
public class AppointmentNotificationListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(AppointmentNotificationListener.class);
    private final BookingNotificationCreator notificationCreator;

    public AppointmentNotificationListener(
            @Qualifier("emailBookingNotificationCreator") BookingNotificationCreator notificationCreator) {
        this.notificationCreator = notificationCreator;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAppointmentBooked(AppointmentBookedEvent event) {
        try {
            // Factory Method chooses the concrete notification sender.
            notificationCreator.notifyBooking(event);
        } catch (Exception ex) {
            // A committed appointment must not fail because mail delivery failed.
            LOGGER.warn("Unable to send booking confirmation for {}: {}", event.appointmentId(), ex.getMessage());
        }
    }
}
