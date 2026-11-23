package com.example.salon.repository;

import com.example.salon.entity.Attendance;
import com.example.salon.entity.Barber;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    Optional<Attendance> findByBarberAndAttendanceDate(Barber barber, LocalDate attendanceDate);

    List<Attendance> findByBarberAndAttendanceDateBetween(Barber barber, LocalDate startDate, LocalDate endDate);

    List<Attendance> findByAttendanceDateBetween(LocalDate startDate, LocalDate endDate);
}
