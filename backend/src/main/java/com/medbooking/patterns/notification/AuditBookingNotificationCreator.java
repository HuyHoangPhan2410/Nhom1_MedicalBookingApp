package com.medbooking.patterns.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Alternate concrete creator, useful for an audit-only workflow (does not send email). */
public class AuditBookingNotificationCreator extends BookingNotificationCreator {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuditBookingNotificationCreator.class);

    @Override
    protected BookingNotification createNotification() {
        return event -> LOGGER.info("Booking {} confirmation event received", event.appointmentId());
    }
}
