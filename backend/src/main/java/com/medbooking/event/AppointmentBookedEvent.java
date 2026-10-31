package com.medbooking.event;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

/**
 * Domain event; its Builder is the Creational Builder pattern.
 * Existing canonical record constructor remains available for compatibility.
 */
public record AppointmentBookedEvent(
        Integer appointmentId, String recipientEmail, String patientName,
        String doctorName, String specialtyName, LocalDate workDate,
        LocalTime startTime, LocalTime endTime, String symptoms,
        String status, BigDecimal consultationFee) {

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private Integer appointmentId;
        private String recipientEmail;
        private String patientName;
        private String doctorName;
        private String specialtyName;
        private LocalDate workDate;
        private LocalTime startTime;
        private LocalTime endTime;
        private String symptoms;
        private String status;
        private BigDecimal consultationFee;

        private Builder() {}

        public Builder appointmentId(Integer value) { appointmentId = value; return this; }
        public Builder recipientEmail(String value) { recipientEmail = value; return this; }
        public Builder patientName(String value) { patientName = value; return this; }
        public Builder doctorName(String value) { doctorName = value; return this; }
        public Builder specialtyName(String value) { specialtyName = value; return this; }
        public Builder workDate(LocalDate value) { workDate = value; return this; }
        public Builder startTime(LocalTime value) { startTime = value; return this; }
        public Builder endTime(LocalTime value) { endTime = value; return this; }
        public Builder symptoms(String value) { symptoms = value; return this; }
        public Builder status(String value) { status = value; return this; }
        public Builder consultationFee(BigDecimal value) { consultationFee = value; return this; }

        public AppointmentBookedEvent build() {
            if (appointmentId == null || appointmentId <= 0) {
                throw new IllegalStateException("Appointment ID must be positive");
            }
            if (recipientEmail == null || recipientEmail.isBlank()) {
                throw new IllegalStateException("Recipient email is required");
            }
            Objects.requireNonNull(workDate, "workDate is required");
            Objects.requireNonNull(startTime, "startTime is required");
            Objects.requireNonNull(endTime, "endTime is required");
            if (!startTime.isBefore(endTime)) {
                throw new IllegalStateException("Start time must be before end time");
            }
            return new AppointmentBookedEvent(appointmentId, recipientEmail, patientName,
                    doctorName, specialtyName, workDate, startTime, endTime,
                    symptoms, status, consultationFee);
        }
    }
}
