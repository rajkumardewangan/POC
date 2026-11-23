package com.example.salon.controller;

import com.example.salon.entity.Attendance;
import com.example.salon.entity.AttendanceStatus;
import com.example.salon.entity.Barber;
import com.example.salon.repository.AttendanceRepository;
import com.example.salon.service.AttendanceService;
import com.example.salon.service.BarberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AttendanceControllerTest {

    private AttendanceRepository attendanceRepository;
    private BarberService barberService;
    private AttendanceService attendanceService;
    private AttendanceController controller;
    private Barber barber;

    @BeforeEach
    void setUp() {
        attendanceRepository = mock(AttendanceRepository.class);
        barberService = mock(BarberService.class);
        attendanceService = mock(AttendanceService.class);
        controller = new AttendanceController(attendanceRepository, barberService, attendanceService);
        barber = new Barber("Asha", "Stylist", "9876543210");
        barber.setId(7L);
        when(barberService.findById(7L)).thenReturn(barber);
    }

    @Test
    void bulkSaveUpdatesExistingAttendance() {
        LocalDate date = LocalDate.of(2026, 10, 1);
        Attendance existing = new Attendance(barber, date, AttendanceStatus.LEAVE);
        when(attendanceRepository.findByBarberAndAttendanceDate(barber, date)).thenReturn(Optional.of(existing));
        Map<String, String> params = Map.of("attendance_7_1", "FULL_DAY");

        String view = controller.saveMonthly(LocalDate.of(2026, 10, 1), params, new RedirectAttributesModelMap());

        assertEquals("redirect:/attendances?date=2026-10-01", view);
        assertEquals(AttendanceStatus.FULL_DAY, existing.getStatus());
        verify(attendanceRepository).save(existing);
    }

    @Test
    void blankBulkValueLeavesExistingAttendanceUnchanged() {
        LocalDate date = LocalDate.of(2026, 10, 1);
        Attendance existing = new Attendance(barber, date, AttendanceStatus.LEAVE);
        when(attendanceRepository.findByBarberAndAttendanceDate(barber, date)).thenReturn(Optional.of(existing));
        Map<String, String> params = new HashMap<>();
        params.put("attendance_7_1", "");

        controller.saveMonthly(LocalDate.of(2026, 10, 1), params, new RedirectAttributesModelMap());

        verify(attendanceRepository, never()).delete(any(Attendance.class));
        verify(attendanceRepository, never()).save(any(Attendance.class));
    }

    @Test
    void explicitClearValueDeletesExistingAttendance() {
        LocalDate date = LocalDate.of(2026, 10, 1);
        Attendance existing = new Attendance(barber, date, AttendanceStatus.LEAVE);
        when(attendanceRepository.findByBarberAndAttendanceDate(barber, date)).thenReturn(Optional.of(existing));
        Map<String, String> params = Map.of("attendance_7_1", "CLEAR");

        controller.saveMonthly(LocalDate.of(2026, 10, 1), params, new RedirectAttributesModelMap());

        verify(attendanceRepository).delete(existing);
        verify(attendanceRepository, never()).save(any(Attendance.class));
    }

    @Test
    void invalidBulkValueDoesNotPartiallySaveSubmission() {
        Map<String, String> params = new HashMap<>();
        params.put("attendance_7_1", "FULL_DAY");
        params.put("attendance_7_2", "NOT_A_STATUS");
        RedirectAttributesModelMap attributes = new RedirectAttributesModelMap();

        controller.saveMonthly(LocalDate.of(2026, 10, 1), params, attributes);

        verify(attendanceRepository, never()).save(any(Attendance.class));
        verify(attendanceRepository, never()).delete(any(Attendance.class));
        assertEquals("Attendance data is invalid. No changes were saved.", attributes.getFlashAttributes().get("error"));
    }

    @Test
    void listExposesSavedStatusNameForCalendarSelection() {
        LocalDate date = LocalDate.of(2026, 10, 25);
        Attendance saved = new Attendance(barber, date, AttendanceStatus.FULL_DAY);
        when(barberService.findAll()).thenReturn(List.of(barber));
        when(attendanceRepository.findByBarberAndAttendanceDateBetween(barber,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31))).thenReturn(List.of(saved));
        when(attendanceService.findByBarberAndMonth(barber, java.time.YearMonth.of(2026, 10))).thenReturn(List.of(saved));
        when(attendanceService.calculateMonthlySalary(barber, java.time.YearMonth.of(2026, 10), List.of(saved)))
                .thenReturn(BigDecimal.ZERO);
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller.list(LocalDate.of(2026, 10, 1), model);

        assertEquals("attendances/list", view);
        @SuppressWarnings("unchecked")
        Map<Long, Map<Integer, String>> attendanceMap = (Map<Long, Map<Integer, String>>) model.getAttribute("attendanceMap");
        assertNotNull(attendanceMap);
        assertEquals("FULL_DAY", attendanceMap.get(7L).get(25));
    }
}