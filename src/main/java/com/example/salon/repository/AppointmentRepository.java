package com.example.salon.repository;

import com.example.salon.entity.Appointment;
import com.example.salon.entity.AppointmentStatus;
import com.example.salon.entity.Barber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

        @Query("SELECT DISTINCT a FROM Appointment a " +
            "JOIN FETCH a.customer " +
            "JOIN FETCH a.barber " +
            "JOIN FETCH a.service " +
            "LEFT JOIN FETCH a.services s " +
            "ORDER BY a.appointmentDateTime DESC")
    List<Appointment> findAllByOrderByAppointmentDateTimeDesc();

        @Query("SELECT DISTINCT a FROM Appointment a " +
            "JOIN FETCH a.customer " +
            "JOIN FETCH a.barber " +
            "JOIN FETCH a.service " +
            "LEFT JOIN FETCH a.services s " +
            "WHERE a.id = :id")
    java.util.Optional<Appointment> findByIdWithDetails(Long id);

        @Query("SELECT DISTINCT a FROM Appointment a " +
            "JOIN FETCH a.service " +
            "LEFT JOIN FETCH a.services s " +
            "WHERE a.barber = :barber " +
            "AND a.appointmentDateTime >= :start AND a.appointmentDateTime < :end")
    List<Appointment> findByBarberAndAppointmentDateTimeBetween(
            @Param("barber") Barber barber,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

        @Query("SELECT DISTINCT a FROM Appointment a " +
            "JOIN FETCH a.customer " +
            "JOIN FETCH a.barber " +
            "JOIN FETCH a.service " +
            "LEFT JOIN FETCH a.services s " +
            "WHERE a.appointmentDateTime >= :start AND a.appointmentDateTime < :end " +
            "ORDER BY a.appointmentDateTime")
    List<Appointment> findByAppointmentDateTimeBetweenOrderByAppointmentDateTime(
            LocalDateTime start, LocalDateTime end);

        @Query("SELECT DISTINCT a FROM Appointment a " +
            "JOIN FETCH a.customer " +
            "JOIN FETCH a.barber " +
            "JOIN FETCH a.service " +
            "LEFT JOIN FETCH a.services s " +
            "WHERE (:barberId IS NULL OR a.barber.id = :barberId) " +
            "AND (:customerName IS NULL OR LOWER(a.customer.name) LIKE LOWER(CONCAT('%', :customerName, '%'))) " +
            "AND (:status IS NULL OR a.status = :status) " +
            "AND a.appointmentDateTime >= :start AND a.appointmentDateTime < :end " +
            "ORDER BY a.appointmentDateTime DESC")
    List<Appointment> findFilteredAppointments(Long barberId, String customerName, AppointmentStatus status, LocalDateTime start, LocalDateTime end);

    long countByServiceId(Long serviceId);
}
