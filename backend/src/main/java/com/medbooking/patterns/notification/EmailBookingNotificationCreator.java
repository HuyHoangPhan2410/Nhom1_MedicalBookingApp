package com.medbooking.patterns.notification;

import com.medbooking.service.AppointmentEmailService;
import org.springframework.stereotype.Component;

/** Production concrete creator: email delivery. */
@Component("emailBookingNotificationCreator")
public class EmailBookingNotificationCreator extends BookingNotificationCreator {
    private final AppointmentEmailService emailService;

    public EmailBookingNotificationCreator(AppointmentEmailService emailService) {
        this.emailService = emailService;
    }

    @Override
    protected BookingNotification createNotification() {
        return emailService::sendBookingConfirmation;
    }
}
