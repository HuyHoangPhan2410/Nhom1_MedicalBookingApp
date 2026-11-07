package com.medbooking.patterns.cancellation;

import com.medbooking.dto.response.CancellationPreviewResponse;
import com.medbooking.entity.Appointment;
import com.medbooking.exception.BusinessException;
import com.medbooking.repository.AppointmentRepository;
import com.medbooking.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** Nạp dữ liệu lịch hẹn và hóa đơn rồi giao cho CancellationAdvisor; không thay đổi dữ liệu. */
@Service
public class CancellationPreviewService {
    private final AppointmentRepository appointmentRepository;
    private final PaymentRepository paymentRepository;
    private final CancellationAdvisor advisor;

    public CancellationPreviewService(AppointmentRepository appointmentRepository,
                                      PaymentRepository paymentRepository,
                                      CancellationAdvisor advisor) {
        this.appointmentRepository = appointmentRepository;
        this.paymentRepository = paymentRepository;
        this.advisor = advisor;
    }

    @Transactional(readOnly = true)
    public CancellationPreviewResponse preview(Integer appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new BusinessException(404, "Lịch hẹn không tồn tại"));
        return advisor.advise(appointment,
                paymentRepository.findByAppointmentId(appointmentId).orElse(null),
                LocalDateTime.now());
    }
}
