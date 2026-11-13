package com.medbooking.service.impl;

import com.medbooking.dto.request.BookAppointmentRequest;
import com.medbooking.dto.response.AppointmentResponse;
import com.medbooking.dto.response.CancellationPreviewResponse;
import com.medbooking.entity.Payment;
import com.medbooking.patterns.cancellation.CancellationAdvisor;
import com.medbooking.patterns.cancellation.state.AppointmentLifecycle;
import com.medbooking.repository.PaymentRepository;
import com.medbooking.entity.Appointment;
import com.medbooking.event.AppointmentBookedEvent;
import com.medbooking.exception.BusinessException;
import com.medbooking.repository.AppointmentRepository;
import com.medbooking.service.AppointmentService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service thực hiện các nghiệp vụ quản lý và đặt lịch hẹn khám bệnh.
 * Đặt/đổi lịch dùng Stored Procedure chống overbooking; hủy lịch dùng giao dịch JPA có khóa hàng.
 */
@Service
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final PaymentRepository paymentRepository;
    private final CancellationAdvisor cancellationAdvisor;
    private final Clock clock;

    public AppointmentServiceImpl(AppointmentRepository appointmentRepository,
                                  ApplicationEventPublisher eventPublisher,
                                  PaymentRepository paymentRepository,
                                  CancellationAdvisor cancellationAdvisor,
                                  Clock clock) {
        this.appointmentRepository = appointmentRepository;
        this.eventPublisher = eventPublisher;
        this.paymentRepository = paymentRepository;
        this.cancellationAdvisor = cancellationAdvisor;
        this.clock = clock;
    }

    /**
     * Đặt lịch hẹn khám bệnh mới.
     * Gọi Stored Procedure sp_BookAppointment để khóa FOR UPDATE và kiểm tra slot trống.
     */
    @Override
    @Transactional
    public AppointmentResponse bookAppointment(BookAppointmentRequest request) {
        // Gọi Stored Procedure dưới MySQL
        Map<String, Object> resultMap = appointmentRepository.spBookAppointment(
                request.getPatientId(),
                request.getDoctorId(),
                request.getScheduleId(),
                request.getSymptoms()
        );

        // Lấy kết quả trả về từ tham số OUT
        Integer appointmentId = (Integer) resultMap.get("p_appointment_id");
        String message = (String) resultMap.get("p_message");

        // Nếu không lấy được ID, ném ra lỗi nghiệp vụ với thông báo từ Database
        if (appointmentId == null) {
            throw new BusinessException(400, message != null ? message : "Đặt lịch hẹn thất bại");
        }

        // Truy vấn thông tin lịch hẹn vừa tạo
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new BusinessException(404, "Không tìm thấy thông tin lịch hẹn sau khi tạo"));

        AppointmentResponse response = mapToResponse(appointment);
        publishBookingConfirmation(appointment);
        return response;
    }

    /**
     * Lấy chi tiết lịch hẹn khám bệnh theo ID.
     */
    @Override
    public AppointmentResponse getAppointmentById(Integer id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "Lịch hẹn không tồn tại"));
        return mapToResponse(appointment);
    }

    /**
     * Lấy danh sách tất cả lịch hẹn khám của một bệnh nhân.
     */
    @Override
    public List<AppointmentResponse> getAppointmentsByPatientId(Integer patientId) {
        return appointmentRepository.findByPatientUserId(patientId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy danh sách tất cả lịch hẹn khám của một bác sĩ.
     */
    @Override
    public List<AppointmentResponse> getAppointmentsByDoctorId(Integer doctorId) {
        return appointmentRepository.findByDoctorUserId(doctorId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /** Read-only advice: State + rule chain + decorated refund estimate. */
    @Override
    @Transactional(readOnly = true)
    public CancellationPreviewResponse previewCancellation(Integer id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "Lịch hẹn không tồn tại"));
        Payment payment = paymentRepository.findByAppointmentId(id).orElse(null);
        return cancellationAdvisor.advise(appointment, payment, LocalDateTime.now(clock));
    }

    /**
     * Re-evaluate rules under row locks; a preview never authorizes a later cancellation.
     * Do not call the legacy self-committing procedure here: validation and updates
     * must share this JPA transaction. No real gateway refund is executed.
     */
    @Override
    @Transactional
    public AppointmentResponse cancelAppointment(Integer id) {
        Appointment appointment = appointmentRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessException(404, "Lịch hẹn không tồn tại"));
        Payment payment = paymentRepository.findByAppointmentIdForUpdate(id).orElse(null);
        CancellationPreviewResponse preview = cancellationAdvisor.advise(
                appointment, payment, LocalDateTime.now(clock));
        if (!preview.allowed()) throw new BusinessException(400, preview.reason());

        AppointmentLifecycle lifecycle = AppointmentLifecycle.of(appointment);
        lifecycle.cancel();
        appointment.setStatus(lifecycle.status());
        // Unpaid invoices can no longer be paid. Keep successful payments as history:
        // cancelling an appointment is NOT evidence of a failed charge or a refund.
        if (payment != null && payment.getStatus() == Payment.Status.pending) {
            payment.setStatus(Payment.Status.failed);
            paymentRepository.save(payment);
        }
        appointmentRepository.save(appointment);
        return mapToResponse(appointment);
    }

    /** Admin uses the same State transitions; cancellation cannot bypass the chain. */
    @Override
    @Transactional
    public AppointmentResponse updateStatus(Integer id, String status) {
        Appointment.Status target;
        try {
            target = Appointment.Status.valueOf(status == null ? "" : status);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(400, "Trạng thái lịch hẹn không hợp lệ");
        }
        if (target == Appointment.Status.cancelled) return cancelAppointment(id);

        Appointment appointment = appointmentRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessException(404, "Lịch hẹn không tồn tại"));
        AppointmentLifecycle lifecycle = AppointmentLifecycle.of(appointment);
        switch (target) {
            case confirmed -> lifecycle.confirm();
            case completed -> lifecycle.complete();
            default -> throw new BusinessException(400, "Không thể chuyển lịch hẹn về trạng thái chờ thanh toán");
        }
        appointment.setStatus(lifecycle.status());
        appointmentRepository.save(appointment);
        return mapToResponse(appointment);
    }

    /**
     * Đổi lịch hẹn sang ca khám mới của cùng bác sĩ.
     * Gọi Stored Procedure sp_RescheduleAppointment để kiểm tra ca mới và giải phóng ca cũ.
     */
    @Override
    @Transactional
    public AppointmentResponse rescheduleAppointment(Integer id, Integer newScheduleId) {
        Appointment current = appointmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "Lịch hẹn không tồn tại"));
        if (!AppointmentLifecycle.of(current).canReschedule()) {
            throw new BusinessException(400, "Lịch hẹn đã hủy hoặc hoàn thành, không thể đổi lịch");
        }
        // The procedure rechecks the status under its own database lock.
        Map<String, Object> resultMap = appointmentRepository.spRescheduleAppointment(id, newScheduleId);

        Object isSuccessObj = resultMap.get("p_is_success");
        String message = (String) resultMap.get("p_message");

        boolean isSuccess = Boolean.TRUE.equals(isSuccessObj) || (isSuccessObj instanceof Number && ((Number) isSuccessObj).intValue() == 1);

        if (!isSuccess) {
            throw new BusinessException(400, message != null ? message : "Đổi lịch hẹn thất bại");
        }

        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "Lịch hẹn không tồn tại"));

        return mapToResponse(appointment);
    }

    /** Publishes the immutable booking snapshot for the existing Observer. */
    private void publishBookingConfirmation(Appointment appointment) {
        if (appointment.getPatient().getUser() == null
                || !StringUtils.hasText(appointment.getPatient().getUser().getEmail())) {
            return;
        }

        // Builder (Creational) assembles an event with many optional fields safely.
        eventPublisher.publishEvent(AppointmentBookedEvent.builder()
                .appointmentId(appointment.getId())
                .recipientEmail(appointment.getPatient().getUser().getEmail())
                .patientName(appointment.getPatient().getFullName())
                .doctorName(appointment.getDoctor().getFullName())
                .specialtyName(appointment.getDoctor().getSpecialty().getName())
                .workDate(appointment.getSchedule().getWorkDate())
                .startTime(appointment.getSchedule().getStartTime())
                .endTime(appointment.getSchedule().getEndTime())
                .symptoms(appointment.getSymptoms())
                .status(appointment.getStatus().name())
                .consultationFee(appointment.getDoctor().getConsultationFee())
                .build());
    }

    private AppointmentResponse mapToResponse(Appointment app) {
        AppointmentResponse res = new AppointmentResponse();
        res.setId(app.getId());
        res.setPatientId(app.getPatient().getUserId());
        res.setPatientName(app.getPatient().getFullName());
        res.setDoctorId(app.getDoctor().getUserId());
        res.setDoctorName(app.getDoctor().getFullName());
        res.setSpecialtyName(app.getDoctor().getSpecialty().getName());
        res.setScheduleId(app.getSchedule().getId());
        res.setWorkDate(app.getSchedule().getWorkDate());
        res.setStartTime(app.getSchedule().getStartTime());
        res.setEndTime(app.getSchedule().getEndTime());
        AppointmentLifecycle lifecycle = AppointmentLifecycle.of(app);
        res.setStatus(lifecycle.status().name());
        res.setCanCancel(lifecycle.canCancel());
        res.setCanReschedule(lifecycle.canReschedule());
        res.setCancelBlockedReason(lifecycle.cancelBlockedReason());
        res.setSymptoms(app.getSymptoms());
        res.setCreatedAt(app.getCreatedAt());
        return res;
    }
}
