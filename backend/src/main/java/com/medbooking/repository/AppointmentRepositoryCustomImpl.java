package com.medbooking.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;

/**
 * Thực thi các Stored Procedure của Appointment bằng EntityManager để đảm bảo tính tương thích và chính xác kiểu dữ liệu.
 */
@Repository
public class AppointmentRepositoryCustomImpl implements AppointmentRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Map<String, Object> spBookAppointment(Integer patientId, Integer doctorId, Integer scheduleId, String symptoms) {
        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("sp_BookAppointment");
        
        // Đăng ký chính xác danh sách tham số IN và OUT theo đúng định nghĩa dưới Database MySQL
        query.registerStoredProcedureParameter("p_patient_id", Integer.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_doctor_id", Integer.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_schedule_id", Integer.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_symptoms", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_appointment_id", Integer.class, ParameterMode.OUT);
        query.registerStoredProcedureParameter("p_message", String.class, ParameterMode.OUT);

        // Đặt giá trị các tham số IN
        query.setParameter("p_patient_id", patientId);
        query.setParameter("p_doctor_id", doctorId);
        query.setParameter("p_schedule_id", scheduleId);
        query.setParameter("p_symptoms", symptoms);

        // Thực thi Stored Procedure
        query.execute();

        // Thu thập kết quả từ các tham số OUT
        Map<String, Object> result = new HashMap<>();
        result.put("p_appointment_id", query.getOutputParameterValue("p_appointment_id"));
        result.put("p_message", query.getOutputParameterValue("p_message"));
        return result;
    }

    @Override
    public Map<String, Object> spConfirmPayment(Integer appointmentId, String paymentMethod) {
        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("sp_ConfirmPayment");
        
        query.registerStoredProcedureParameter("p_appointment_id", Integer.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_payment_method", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_is_success", Boolean.class, ParameterMode.OUT);
        query.registerStoredProcedureParameter("p_message", String.class, ParameterMode.OUT);

        query.setParameter("p_appointment_id", appointmentId);
        query.setParameter("p_payment_method", paymentMethod);

        query.execute();

        Map<String, Object> result = new HashMap<>();
        result.put("p_is_success", query.getOutputParameterValue("p_is_success"));
        result.put("p_message", query.getOutputParameterValue("p_message"));
        return result;
    }

    @Override
    public Map<String, Object> spCancelAppointment(Integer appointmentId, String cancelledByRole) {
        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("sp_CancelAppointment");
        
        query.registerStoredProcedureParameter("p_appointment_id", Integer.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_cancelled_by_role", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_is_success", Boolean.class, ParameterMode.OUT);
        query.registerStoredProcedureParameter("p_message", String.class, ParameterMode.OUT);

        query.setParameter("p_appointment_id", appointmentId);
        query.setParameter("p_cancelled_by_role", cancelledByRole);

        query.execute();

        Map<String, Object> result = new HashMap<>();
        result.put("p_is_success", query.getOutputParameterValue("p_is_success"));
        result.put("p_message", query.getOutputParameterValue("p_message"));
        return result;
    }

    @Override
    public Map<String, Object> spRescheduleAppointment(Integer appointmentId, Integer newScheduleId) {
        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("sp_RescheduleAppointment");
        
        query.registerStoredProcedureParameter("p_appointment_id", Integer.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_new_schedule_id", Integer.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_is_success", Boolean.class, ParameterMode.OUT);
        query.registerStoredProcedureParameter("p_message", String.class, ParameterMode.OUT);

        query.setParameter("p_appointment_id", appointmentId);
        query.setParameter("p_new_schedule_id", newScheduleId);

        query.execute();

        Map<String, Object> result = new HashMap<>();
        result.put("p_is_success", query.getOutputParameterValue("p_is_success"));
        result.put("p_message", query.getOutputParameterValue("p_message"));
        // The prechecked Appointment may still be managed with its old Schedule.
        entityManager.clear();
        return result;
    }
}
