package com.medbooking.patterns;

import com.medbooking.event.AppointmentBookedEvent;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.LocalTime;
import static org.junit.jupiter.api.Assertions.*;

class AppointmentBookedEventBuilderTest {
    @Test
    void builderCreatesEventWithRequiredAndOptionalFields() {
        AppointmentBookedEvent event = AppointmentBookedEvent.builder()
                .appointmentId(42).recipientEmail("patient@example.com")
                .workDate(LocalDate.of(2026, 11, 5))
                .startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(10, 0))
                .patientName("Nguyen Van A").build();
        assertEquals(42, event.appointmentId());
        assertEquals("Nguyen Van A", event.patientName());
        assertNull(event.consultationFee());
    }

    @Test
    void builderRejectsInvalidDatesAndMissingRequiredFields() {
        assertThrows(IllegalStateException.class, () -> AppointmentBookedEvent.builder()
                .appointmentId(-1).recipientEmail("a@example.com")
                .workDate(LocalDate.now()).startTime(LocalTime.NOON)
                .endTime(LocalTime.MIDNIGHT).build());
        assertThrows(NullPointerException.class, () -> AppointmentBookedEvent.builder()
                .appointmentId(7).recipientEmail("a@example.com")
                .startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(10, 0)).build());
        assertThrows(IllegalStateException.class, () -> AppointmentBookedEvent.builder()
                .appointmentId(7).recipientEmail("a@example.com")
                .workDate(LocalDate.now()).startTime(LocalTime.of(11, 0))
                .endTime(LocalTime.of(10, 0)).build());
    }
}
