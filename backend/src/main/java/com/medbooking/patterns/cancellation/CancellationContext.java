package com.medbooking.patterns.cancellation;

import com.medbooking.entity.Appointment;
import com.medbooking.entity.Payment;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Dữ liệu đầu vào dùng chung cho chuỗi kiểm tra hủy lịch và bộ tính hoàn tiền.
 * payment có thể null (lịch hẹn chưa tạo hóa đơn).
 */
public record CancellationContext(Appointment appointment, Payment payment, LocalDateTime now) {

    /** Thời điểm bắt đầu ca khám. */
    public LocalDateTime startAt() {
        return LocalDateTime.of(
                appointment.getSchedule().getWorkDate(),
                appointment.getSchedule().getStartTime());
    }

    /** Khoảng thời gian còn lại đến giờ khám (âm nếu đã qua giờ khám). */
    public Duration timeUntilStart() {
        return Duration.between(now, startAt());
    }

    public boolean isPaid() {
        return payment != null && payment.getStatus() == Payment.Status.success;
    }
}
