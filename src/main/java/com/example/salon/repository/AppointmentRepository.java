package com.example.salon.repository;

import com.example.salon.entity.Appointment;
import com.example.salon.entity.Barber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    @Query("SELECT a FROM Appointment a " +
            "JOIN FETCH a.customer " +
            "JOIN FETCH a.barber " +
            "JOIN FETCH a.service " +
            "ORDER BY a.appointmentDateTime DESC")
    List<Appointment> findAllByOrderByAppointmentDateTimeDesc();

    List<Appointment> findByBarberAndAppointmentDateTimeBetween(
            Barber barber, LocalDateTime start, LocalDateTime end);

    @Query("SELECT a FROM Appointment a " +
            "JOIN FETCH a.customer " +
            "JOIN FETCH a.barber " +
            "JOIN FETCH a.service " +
            "WHERE a.appointmentDateTime >= :start AND a.appointmentDateTime < :end " +
            "ORDER BY a.appointmentDateTime")
    List<Appointment> findByAppointmentDateTimeBetweenOrderByAppointmentDateTime(
            LocalDateTime start, LocalDateTime end);
}
