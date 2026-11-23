package com.example.salon.service;

import com.example.salon.entity.Attendance;
import com.example.salon.entity.AttendanceStatus;
import com.example.salon.entity.Barber;
import com.example.salon.repository.AttendanceRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;

    public AttendanceService(AttendanceRepository attendanceRepository) {
        this.attendanceRepository = attendanceRepository;
    }

    public AttendanceService() {
        this.attendanceRepository = null;
    }

    public Attendance save(Attendance attendance) {
        if (attendanceRepository == null) {
            throw new IllegalStateException("AttendanceRepository is not configured.");
        }
        return attendanceRepository.save(attendance);
    }

    public Attendance findByBarberAndDate(Barber barber, LocalDate date) {
        if (attendanceRepository == null) {
            return null;
        }
        return attendanceRepository.findByBarberAndAttendanceDate(barber, date).orElse(null);
    }

    public List<Attendance> findByBarberAndMonth(Barber barber, YearMonth month) {
        if (attendanceRepository == null) {
            return List.of();
        }
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();
        return attendanceRepository.findByBarberAndAttendanceDateBetween(barber, start, end);
    }

    public BigDecimal calculateMonthlySalary(Barber barber, YearMonth month, List<Attendance> records) {
        if (barber == null || barber.getBaseSalary() == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal grossSalary = barber.getGrossMonthlySalary();
        BigDecimal fixedDeduction = barber.getFixedSalaryDeduction();
        Map<LocalDate, Attendance> attendanceByDate = records.stream()
                .filter(record -> record != null && record.getAttendanceDate() != null)
                .filter(record -> YearMonth.from(record.getAttendanceDate()).equals(month))
                .collect(Collectors.toMap(Attendance::getAttendanceDate, Function.identity(), (existing, replacement) -> replacement));

        long fullDayCount = attendanceByDate.values().stream()
                .filter(record -> record.getStatus() == AttendanceStatus.FULL_DAY)
                .count();
        long halfDayCount = attendanceByDate.values().stream()
                .filter(record -> record.getStatus() == AttendanceStatus.HALF_DAY)
                .count();
        long leaveCount = attendanceByDate.values().stream()
                .filter(record -> record.getStatus() == AttendanceStatus.LEAVE)
                .count();
        long weeklyOffCount = attendanceByDate.values().stream()
                .filter(record -> record.getStatus() == AttendanceStatus.WEEKLY_OFF)
                .count();

        BigDecimal deductions = BigDecimal.ZERO;
        if (leaveCount > 2) {
            deductions = deductions.add(BigDecimal.valueOf(leaveCount - 2)
                    .multiply(grossSalary)
                    .divide(BigDecimal.valueOf(month.lengthOfMonth()), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(1.5)));
        }

        deductions = deductions.add(BigDecimal.valueOf(halfDayCount)
                .multiply(grossSalary)
                .divide(BigDecimal.valueOf(month.lengthOfMonth()), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(0.5)));

        if (fullDayCount == 0 && halfDayCount == 0 && leaveCount == 0 && weeklyOffCount == 0) {
            return grossSalary.subtract(fixedDeduction).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        }

        if (fullDayCount == 0 && halfDayCount == 0 && leaveCount == 0) {
            return grossSalary.subtract(fixedDeduction).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        }

        return grossSalary.subtract(fixedDeduction).subtract(deductions).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }
}
