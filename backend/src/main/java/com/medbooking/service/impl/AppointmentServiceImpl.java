package com.medbooking.service.impl;

import com.medbooking.dto.request.BookAppointmentRequest;
import com.medbooking.dto.response.AppointmentResponse;
import com.medbooking.entity.Appointment;
import com.medbooking.event.AppointmentBookedEvent;
import com.medbooking.exception.BusinessException;
import com.medbooking.repository.AppointmentRepository;
import com.medbooking.service.AppointmentService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service thực hiện các nghiệp vụ quản lý và đặt lịch hẹn khám bệnh.
 * Sử dụng Stored Procedures dưới MySQL để đảm bảo an toàn giao dịch và chống overbooking.
 */
@Service
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final ApplicationEventPublisher eventPublisher;

    public AppointmentServiceImpl(AppointmentRepository appointmentRepository,
                                  ApplicationEventPublisher eventPublisher) {
        this.appointmentRepository = appointmentRepository;
        this.eventPublisher = eventPublisher;
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

    /**
     * Hủy lịch hẹn khám bệnh.
     * Gọi Stored Procedure sp_CancelAppointment để cập nhật trạng thái lịch hẹn và hóa đơn thanh toán.
     */
    @Override
    @Transactional
    public AppointmentResponse cancelAppointment(Integer id) {
        // Gọi Stored Procedure hủy lịch
        Map<String, Object> resultMap = appointmentRepository.spCancelAppointment(id, "patient");

        Object isSuccessObj = resultMap.get("p_is_success");
        String message = (String) resultMap.get("p_message");

        boolean isSuccess = Boolean.TRUE.equals(isSuccessObj) || (isSuccessObj instanceof Number && ((Number) isSuccessObj).intValue() == 1);

        if (!isSuccess) {
            throw new BusinessException(400, message != null ? message : "Hủy lịch hẹn thất bại");
        }

        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "Lịch hẹn không tồn tại"));

        return mapToResponse(appointment);
    }

    /**
     * Đổi lịch hẹn sang ca khám mới của cùng bác sĩ.
     * Gọi Stored Procedure sp_RescheduleAppointment để kiểm tra ca mới và giải phóng ca cũ.
     */
    @Override
    @Transactional
    public AppointmentResponse rescheduleAppointment(Integer id, Integer newScheduleId) {
        // Gọi Stored Procedure đổi lịch
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

    /**
     * Chuyển đổi từ Entity Appointment sang DTO AppointmentResponse.
     */
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
        res.setStatus(app.getStatus().name());
        res.setSymptoms(app.getSymptoms());
        res.setCreatedAt(app.getCreatedAt());
        return res;
    }
}
