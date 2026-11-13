package com.medbooking.patterns.cancellation;

import com.medbooking.entity.Appointment;
import com.medbooking.entity.Payment;
import com.medbooking.entity.Schedule;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Dựng nhanh dữ liệu mẫu cho các test của nhóm pattern hủy lịch. */
final class CancellationTestSupport {
    static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 10, 8, 0);

    private CancellationTestSupport() {}

    static Appointment appointment(Appointment.Status status, LocalDateTime startAt) {
        Schedule schedule = new Schedule();
        schedule.setWorkDate(startAt.toLocalDate());
        schedule.setStartTime(startAt.toLocalTime());
        schedule.setEndTime(startAt.toLocalTime().plusHours(1));
        Appointment appointment = new Appointment();
        appointment.setId(100);
        appointment.setStatus(status);
        appointment.setSchedule(schedule);
        return appointment;
    }

    static Payment payment(Payment.PaymentMethod method, Payment.Status status, String amount) {
        Payment payment = new Payment();
        payment.setPaymentMethod(method);
        payment.setStatus(status);
        payment.setAmount(new BigDecimal(amount));
        return payment;
    }
}
