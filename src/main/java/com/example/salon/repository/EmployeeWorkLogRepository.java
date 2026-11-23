package com.example.salon.repository;

import com.example.salon.entity.EmployeeWorkLog;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeWorkLogRepository extends JpaRepository<EmployeeWorkLog, Long> {

    @EntityGraph(attributePaths = {"barber", "service", "appointment", "appointment.services"})
    List<EmployeeWorkLog> findByEndedAtIsNullOrderByStartedAtDesc();

    @EntityGraph(attributePaths = {"barber", "service", "appointment", "appointment.services"})
    Optional<EmployeeWorkLog> findFirstByBarberIdAndEndedAtIsNullOrderByStartedAtDesc(Long barberId);

    @EntityGraph(attributePaths = {"barber", "service", "appointment", "appointment.services"})
    List<EmployeeWorkLog> findByAppointmentIdAndBarberIdAndEndedAtIsNull(Long appointmentId, Long barberId);
}