package com.example.salon.service;

import com.example.salon.controller.HomeController;
import com.example.salon.entity.Attendance;
import com.example.salon.entity.AttendanceStatus;
import com.example.salon.entity.Barber;
import com.example.salon.repository.AppointmentRepository;
import com.example.salon.repository.AttendanceRepository;
import com.example.salon.repository.BarberRepository;
import com.example.salon.repository.CustomerRepository;
import com.example.salon.repository.SalonServiceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AttendanceServiceTest {

    @Test
    void calculatesSalaryForFullMonthWithoutDeductions() {
        Barber barber = new Barber("Raj", "Haircut", "9999999999");
        barber.setBaseSalary(new BigDecimal("30000"));

        AttendanceService service = new AttendanceService();
        List<Attendance> records = List.of(
                new Attendance(barber, LocalDate.of(2026, 9, 1), AttendanceStatus.FULL_DAY),
                new Attendance(barber, LocalDate.of(2026, 9, 2), AttendanceStatus.FULL_DAY),
                new Attendance(barber, LocalDate.of(2026, 9, 3), AttendanceStatus.FULL_DAY),
                new Attendance(barber, LocalDate.of(2026, 9, 4), AttendanceStatus.FULL_DAY),
                new Attendance(barber, LocalDate.of(2026, 9, 5), AttendanceStatus.FULL_DAY)
        );

        assertEquals(new BigDecimal("30000.00"), service.calculateMonthlySalary(barber, YearMonth.of(2026, 9), records));
    }

    @Test
    void deductsHalfDayAndExtraLeaveFromSalary() {
        Barber barber = new Barber("Raj", "Haircut", "9999999999");
        barber.setBaseSalary(new BigDecimal("30000"));

        AttendanceService service = new AttendanceService();
        List<Attendance> records = List.of(
                new Attendance(barber, LocalDate.of(2026, 9, 1), AttendanceStatus.FULL_DAY),
                new Attendance(barber, LocalDate.of(2026, 9, 2), AttendanceStatus.HALF_DAY),
                new Attendance(barber, LocalDate.of(2026, 9, 3), AttendanceStatus.LEAVE),
                new Attendance(barber, LocalDate.of(2026, 9, 4), AttendanceStatus.LEAVE),
                new Attendance(barber, LocalDate.of(2026, 9, 5), AttendanceStatus.LEAVE),
                new Attendance(barber, LocalDate.of(2026, 9, 6), AttendanceStatus.LEAVE)
        );

        assertEquals(new BigDecimal("26500.00"), service.calculateMonthlySalary(barber, YearMonth.of(2026, 9), records));
    }

    @Test
    void weeklyOffDoesNotReduceSalary() {
        Barber barber = new Barber("Raj", "Haircut", "9999999999");
        barber.setBaseSalary(new BigDecimal("30000"));

        AttendanceService service = new AttendanceService();
        List<Attendance> records = List.of(
                new Attendance(barber, LocalDate.of(2026, 9, 1), AttendanceStatus.FULL_DAY),
                new Attendance(barber, LocalDate.of(2026, 9, 6), AttendanceStatus.WEEKLY_OFF)
        );

        assertEquals(new BigDecimal("30000.00"), service.calculateMonthlySalary(barber, YearMonth.of(2026, 9), records));
    }

        @Test
        void includesAllowancesAndSubtractsFixedDeductionFromNetSalary() {
                Barber barber = new Barber("Raj", "Haircut", "9999999999");
                barber.setBaseSalary(new BigDecimal("30000"));
                barber.setHousingAllowance(new BigDecimal("2000"));
                barber.setTransportAllowance(new BigDecimal("1000"));
                barber.setFixedSalaryDeduction(new BigDecimal("500"));

                AttendanceService service = new AttendanceService();

                assertEquals(new BigDecimal("32500.00"),
                                service.calculateMonthlySalary(barber, YearMonth.of(2026, 9), List.of()));
        }

    @Test
    void homeController_payrollReport_shouldListMonthlyPayrollSummary() {
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        BarberRepository barberRepository = mock(BarberRepository.class);
        SalonServiceRepository salonServiceRepository = mock(SalonServiceRepository.class);
        AppointmentRepository appointmentRepository = mock(AppointmentRepository.class);
        AttendanceRepository attendanceRepository = mock(AttendanceRepository.class);

        Barber barber = new Barber("Raj", "Haircut", "9999999999");
        barber.setId(10L);
        barber.setBaseSalary(new BigDecimal("30000"));

        when(barberRepository.findAll()).thenReturn(List.of(barber));
        YearMonth currentMonth = YearMonth.now();
        when(attendanceRepository.findByBarberAndAttendanceDateBetween(eq(barber), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(
                        new Attendance(barber, currentMonth.atDay(1), AttendanceStatus.FULL_DAY),
                        new Attendance(barber, currentMonth.atDay(2), AttendanceStatus.HALF_DAY)
                ));

        HomeController controller = new HomeController(
                customerRepository,
                barberRepository,
                salonServiceRepository,
                appointmentRepository,
                attendanceRepository,
                new AttendanceService(attendanceRepository));

        Model model = new ExtendedModelMap();
        String view = controller.payrollReport(model);

        assertEquals("reports/payroll", view);
        assertEquals(1, ((List<?>) model.getAttribute("payrollRows")).size());
        BigDecimal expectedNetSalary = new BigDecimal("30000")
                .subtract(new BigDecimal("30000").divide(BigDecimal.valueOf(currentMonth.lengthOfMonth()), 4, java.math.RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("0.5")))
                .setScale(2, java.math.RoundingMode.HALF_UP);
        assertEquals(expectedNetSalary, ((HomeController.PayrollSummaryRow) ((List<?>) model.getAttribute("payrollRows")).get(0)).getNetSalary());
    }
}
