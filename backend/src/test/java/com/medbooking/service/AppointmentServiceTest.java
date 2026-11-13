package com.medbooking.service;

import com.medbooking.dto.request.BookAppointmentRequest;
import com.medbooking.dto.response.AppointmentResponse;
import com.medbooking.event.AppointmentBookedEvent;
import com.medbooking.entity.*;
import com.medbooking.exception.BusinessException;
import com.medbooking.repository.AppointmentRepository;
import com.medbooking.service.impl.AppointmentServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.ApplicationEventPublisher;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.BeforeEach;
import com.medbooking.patterns.cancellation.CancellationAdvisor;
import com.medbooking.repository.PaymentRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Kiểm thử đơn vị (Unit Test) cho các chức năng quản lý lịch hẹn khám bệnh.
 */
@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private PaymentRepository paymentRepository;

    private AppointmentServiceImpl appointmentService;

    @BeforeEach
    void setUp() {
        appointmentService = new AppointmentServiceImpl(appointmentRepository, eventPublisher,
                paymentRepository, new CancellationAdvisor(),
                Clock.fixed(Instant.parse("2026-11-12T00:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh")));
    }

    @Test
    @DisplayName("Kiểm thử đặt lịch hẹn thành công khi ca khám còn trống")
    void testBookAppointment_Success() {
        BookAppointmentRequest request = new BookAppointmentRequest();
        request.setPatientId(1);
        request.setDoctorId(2);
        request.setScheduleId(3);
        request.setSymptoms("Sốt cao");

        Map<String, Object> procResult = new HashMap<>();
        procResult.put("p_appointment_id", 100);
        procResult.put("p_message", "Đặt lịch thành công");

        when(appointmentRepository.spBookAppointment(1, 2, 3, "Sốt cao")).thenReturn(procResult);

        Appointment app = createDummyAppointment(100, 1, 2);
        when(appointmentRepository.findById(100)).thenReturn(Optional.of(app));

        AppointmentResponse response = appointmentService.bookAppointment(request);

        assertNotNull(response, "Kết quả phản hồi không được null");
        assertEquals(100, response.getId(), "Mã lịch hẹn phải là 100");
        assertTrue(response.isCanCancel());
        assertTrue(response.isCanReschedule());
        assertEquals("Nguyễn Văn A", response.getPatientName(), "Tên bệnh nhân phải trùng khớp");

        ArgumentCaptor<AppointmentBookedEvent> eventCaptor = ArgumentCaptor.forClass(AppointmentBookedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertEquals("patient@example.com", eventCaptor.getValue().recipientEmail());
        assertEquals(new BigDecimal("500000"), eventCaptor.getValue().consultationFee());
    }

    @Test
    @DisplayName("Kiểm thử đặt lịch thất bại khi ca khám đã hết chỗ (Overbooking)")
    void testBookAppointment_Failure_Overbooking() {
        BookAppointmentRequest request = new BookAppointmentRequest();
        request.setPatientId(1);
        request.setDoctorId(2);
        request.setScheduleId(3);

        Map<String, Object> procResult = new HashMap<>();
        procResult.put("p_appointment_id", null);
        procResult.put("p_message", "Thất bại: Ca khám này đã hết chỗ.");

        when(appointmentRepository.spBookAppointment(1, 2, 3, null)).thenReturn(procResult);

        BusinessException ex = assertThrows(BusinessException.class, () -> appointmentService.bookAppointment(request));
        assertTrue(ex.getMessage().contains("hết chỗ"), "Thông báo lỗi phải chứa thông tin hết chỗ");
    }

    @Test
    @DisplayName("Kiểm thử hủy lịch hẹn khám thành công")
    void testCancelAppointment_Success() {
        Appointment app = createDummyAppointment(100, 1, 2);
        app.getSchedule().setWorkDate(LocalDate.of(2026, 11, 13));
        when(appointmentRepository.findByIdForUpdate(100)).thenReturn(Optional.of(app));

        AppointmentResponse response = appointmentService.cancelAppointment(100);

        assertNotNull(response);
        assertEquals("cancelled", response.getStatus());
        assertFalse(response.isCanCancel());
        assertFalse(response.isCanReschedule());
        verify(appointmentRepository).save(app);
    }

    @Test
    @DisplayName("Kiểm thử đổi lịch khám thành công")
    void testRescheduleAppointment_Success() {
        Map<String, Object> procResult = new HashMap<>();
        procResult.put("p_is_success", 1);
        procResult.put("p_message", "Đổi lịch thành công");

        when(appointmentRepository.spRescheduleAppointment(100, 4)).thenReturn(procResult);

        Appointment app = createDummyAppointment(100, 1, 2);
        Schedule newSchedule = new Schedule();
        newSchedule.setId(4);
        newSchedule.setWorkDate(LocalDate.now().plusDays(1));
        newSchedule.setStartTime(LocalTime.of(14, 0));
        newSchedule.setEndTime(LocalTime.of(15, 30));
        app.setSchedule(newSchedule);

        when(appointmentRepository.findById(100)).thenReturn(Optional.of(app));

        AppointmentResponse response = appointmentService.rescheduleAppointment(100, 4);

        assertNotNull(response);
        assertEquals(4, response.getScheduleId());
    }

    @Test
    @DisplayName("Kiểm thử tra cứu danh sách lịch hẹn của bệnh nhân")
    void testGetAppointmentsByPatientId() {
        Appointment app = createDummyAppointment(100, 1, 2);
        when(appointmentRepository.findByPatientUserId(1)).thenReturn(List.of(app));

        List<AppointmentResponse> list = appointmentService.getAppointmentsByPatientId(1);

        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("Nguyễn Văn A", list.get(0).getPatientName());
    }

    @Test
    @DisplayName("Kiểm thử tra cứu danh sách lịch hẹn của bác sĩ")
    void testGetAppointmentsByDoctorId() {
        Appointment app = createDummyAppointment(100, 1, 2);
        when(appointmentRepository.findByDoctorUserId(2)).thenReturn(List.of(app));

        List<AppointmentResponse> list = appointmentService.getAppointmentsByDoctorId(2);

        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("Bác sĩ B", list.get(0).getDoctorName());
    }

    private Appointment createDummyAppointment(Integer appID, Integer patientId, Integer doctorId) {
        Appointment app = new Appointment();
        app.setId(appID);

        Patient patient = new Patient();
        patient.setUserId(patientId);
        patient.setFullName("Nguyễn Văn A");
        User patientUser = new User();
        patientUser.setId(patientId);
        patientUser.setEmail("patient@example.com");
        patient.setUser(patientUser);
        app.setPatient(patient);

        Doctor doctor = new Doctor();
        doctor.setUserId(doctorId);
        doctor.setFullName("Bác sĩ B");
        doctor.setConsultationFee(new BigDecimal("500000"));
        Specialty specialty = new Specialty();
        specialty.setName("Nội khoa");
        doctor.setSpecialty(specialty);
        app.setDoctor(doctor);

        Schedule schedule = new Schedule();
        schedule.setId(3);
        schedule.setWorkDate(LocalDate.now());
        schedule.setStartTime(LocalTime.of(8, 0));
        schedule.setEndTime(LocalTime.of(9, 30));
        app.setSchedule(schedule);

        app.setStatus(Appointment.Status.pending);
        return app;
    }
}
