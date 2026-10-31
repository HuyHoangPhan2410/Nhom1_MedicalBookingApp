package com.medbooking.event;

import com.medbooking.service.AppointmentEmailService;
import com.medbooking.patterns.notification.EmailBookingNotificationCreator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AppointmentNotificationListenerTest {

    @Mock
    private AppointmentEmailService appointmentEmailService;

    @Test
    @DisplayName("Listener g\u1eedi email x\u00e1c nh\u1eadn sau khi nh\u1eadn s\u1ef1 ki\u1ec7n \u0111\u1eb7t l\u1ecbch")
    void listenerSendsConfirmationEmail() {
        AppointmentBookedEvent event = createEvent();
        AppointmentNotificationListener listener = new AppointmentNotificationListener(new EmailBookingNotificationCreator(appointmentEmailService));

        listener.handleAppointmentBooked(event);

        verify(appointmentEmailService).sendBookingConfirmation(event);
    }

    @Test
    @DisplayName("L\u1ed7i g\u1eedi email kh\u00f4ng l\u00e0m l\u1ed7i lu\u1ed3ng \u0111\u1eb7t l\u1ecbch \u0111\u00e3 commit")
    void listenerDoesNotPropagateMailFailure() {
        AppointmentBookedEvent event = createEvent();
        AppointmentNotificationListener listener = new AppointmentNotificationListener(new EmailBookingNotificationCreator(appointmentEmailService));
        doThrow(new IllegalStateException("SMTP unavailable"))
                .when(appointmentEmailService).sendBookingConfirmation(event);

        assertDoesNotThrow(() -> listener.handleAppointmentBooked(event));
    }

    private AppointmentBookedEvent createEvent() {
        return new AppointmentBookedEvent(
                123,
                "patient@gmail.com",
                "Nguy\u1ec5n V\u0103n A",
                "BS. Tr\u1ea7n V\u0103n B",
                "N\u1ed9i khoa",
                LocalDate.of(2026, 8, 15),
                LocalTime.of(8, 0),
                LocalTime.of(9, 30),
                "\u0110au \u0111\u1ea7u",
                "pending",
                new BigDecimal("500000")
        );
    }
}
