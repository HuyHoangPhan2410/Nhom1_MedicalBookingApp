package com.medbooking.service;

import com.medbooking.dto.request.BookAppointmentRequest;
import com.medbooking.dto.response.AppointmentResponse;
import com.medbooking.dto.response.CancellationPreviewResponse;

import java.util.List;

public interface AppointmentService {
    AppointmentResponse bookAppointment(BookAppointmentRequest request);
    AppointmentResponse getAppointmentById(Integer id);
    List<AppointmentResponse> getAppointmentsByPatientId(Integer patientId);
    List<AppointmentResponse> getAppointmentsByDoctorId(Integer doctorId);
    CancellationPreviewResponse previewCancellation(Integer id);
    AppointmentResponse cancelAppointment(Integer id);
    AppointmentResponse updateStatus(Integer id, String status);
    AppointmentResponse rescheduleAppointment(Integer id, Integer newScheduleId);
}
