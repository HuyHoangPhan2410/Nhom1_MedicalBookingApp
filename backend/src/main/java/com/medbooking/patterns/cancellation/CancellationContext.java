package com.medbooking.patterns.cancellation;

import com.medbooking.entity.Appointment;
import com.medbooking.entity.Payment;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

public record CancellationContext(Appointment appointment, Payment payment, LocalDateTime now) {
    public CancellationContext {
        Objects.requireNonNull(appointment, "Appointment is required");
        Objects.requireNonNull(now, "Evaluation time is required");
    }
    public LocalDateTime startAt() {
        return appointment.getSchedule().getWorkDate().atTime(appointment.getSchedule().getStartTime());
    }
    public Duration timeUntilStart() { return Duration.between(now, startAt()); }
    public boolean isPaid() { return payment != null && payment.getStatus() == Payment.Status.success; }
}
