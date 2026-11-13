package com.medbooking.patterns;

import com.medbooking.event.AppointmentBookedEvent;
import com.medbooking.patterns.notification.AuditBookingNotificationCreator;
import com.medbooking.patterns.notification.EmailBookingNotificationCreator;
import com.medbooking.service.AppointmentEmailService;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.LocalTime;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

class BookingNotificationCreatorTest {
    @Test
    void emailCreatorMakesEmailNotification() {
        AppointmentEmailService email = mock(AppointmentEmailService.class);
        AppointmentBookedEvent event = event();
        new EmailBookingNotificationCreator(email).notifyBooking(event);
        verify(email).sendBookingConfirmation(event);
    }

    @Test
    void alternateCreatorUsesAuditInsteadOfMail() {
        assertDoesNotThrow(() -> new AuditBookingNotificationCreator().notifyBooking(event()));
    }

    private AppointmentBookedEvent event() {
        return AppointmentBookedEvent.builder().appointmentId(5)
                .recipientEmail("patient@example.com").workDate(LocalDate.of(2026, 11, 5))
                .startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(10, 0)).build();
    }
}
