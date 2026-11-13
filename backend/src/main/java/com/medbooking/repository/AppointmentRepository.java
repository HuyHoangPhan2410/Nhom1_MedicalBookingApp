package com.medbooking.repository;

import com.medbooking.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository quản lý entity Appointment và các Stored Procedure liên quan.
 */
@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Integer>, AppointmentRepositoryCustom {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Appointment a where a.id = :id")
    Optional<Appointment> findByIdForUpdate(@Param("id") Integer id);

    List<Appointment> findByPatientUserId(Integer patientId);
    List<Appointment> findByDoctorUserId(Integer doctorId);
}
