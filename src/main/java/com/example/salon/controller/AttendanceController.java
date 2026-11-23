package com.example.salon.controller;

import com.example.salon.entity.Attendance;
import com.example.salon.entity.AttendanceStatus;
import com.example.salon.entity.Barber;
import com.example.salon.repository.AttendanceRepository;
import com.example.salon.service.AttendanceService;
import com.example.salon.service.BarberService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

@Controller
@RequestMapping("/attendances")
public class AttendanceController {

    private final AttendanceRepository attendanceRepository;
    private final BarberService barberService;
    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceRepository attendanceRepository,
                               BarberService barberService,
                               AttendanceService attendanceService) {
        this.attendanceRepository = attendanceRepository;
        this.barberService = barberService;
        this.attendanceService = attendanceService;
    }

    @GetMapping
    public String list(@RequestParam(required = false)
                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                       Model model) {
        YearMonth month = (date != null ? YearMonth.from(date) : YearMonth.now());
        List<Barber> barbers = barberService.findAll().stream()
                .sorted(Comparator.comparing(Barber::getName))
                .toList();

        Map<Long, Map<Integer, String>> attendanceMap = new HashMap<>();
        for (Barber barber : barbers) {
            Map<Integer, String> dailyStatus = new HashMap<>();
            attendanceRepository.findByBarberAndAttendanceDateBetween(barber, month.atDay(1), month.atEndOfMonth())
                .forEach(entry -> dailyStatus.put(entry.getAttendanceDate().getDayOfMonth(), entry.getStatus().name()));

            for (int day = 1; day <= month.lengthOfMonth(); day++) {
                LocalDate currentDate = month.atDay(day);
                if (dailyStatus.containsKey(day)) {
                    continue;
                }
                if (barber.getWeeklyOffDay() != null && currentDate.getDayOfWeek() == barber.getWeeklyOffDay()) {
                    dailyStatus.put(day, AttendanceStatus.WEEKLY_OFF.name());
                }
            }

            attendanceMap.put(barber.getId(), dailyStatus);
        }

        List<AttendanceSummary> summaries = barbers.stream()
                .map(barber -> {
                    List<Attendance> monthRecords = attendanceService.findByBarberAndMonth(barber, month);
                    BigDecimal salary = attendanceService.calculateMonthlySalary(barber, month, monthRecords);
                    long fullDays = monthRecords.stream().filter(a -> a.getStatus() == AttendanceStatus.FULL_DAY).count();
                    long halfDays = monthRecords.stream().filter(a -> a.getStatus() == AttendanceStatus.HALF_DAY).count();
                    long leaveDays = monthRecords.stream().filter(a -> a.getStatus() == AttendanceStatus.LEAVE).count();
                    long weeklyOffDays = monthRecords.stream().filter(a -> a.getStatus() == AttendanceStatus.WEEKLY_OFF).count();
                    return new AttendanceSummary(barber, month, salary, fullDays, halfDays, leaveDays, weeklyOffDays);
                })
                .toList();

        Map<Long, BigDecimal> salaryByBarberId = summaries.stream()
                .collect(java.util.stream.Collectors.toMap(summary -> summary.barber().getId(), summary -> summary.salary()));

        model.addAttribute("selectedMonth", month);
        model.addAttribute("summaries", summaries);
        model.addAttribute("salaryByBarberId", salaryByBarberId);
        model.addAttribute("barbers", barbers);
        model.addAttribute("attendanceMap", attendanceMap);
        model.addAttribute("days", IntStream.rangeClosed(1, month.lengthOfMonth()).boxed().toList());
        model.addAttribute("statusOptions", AttendanceStatus.values());
        model.addAttribute("selectedDate", date != null ? date : LocalDate.now());
        return "attendances/list";
    }

    @PostMapping("/bulk")
    @Transactional
    public String saveMonthly(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate monthDate,
                             @RequestParam Map<String, String> allParams,
                             RedirectAttributes redirectAttributes) {
        YearMonth month = YearMonth.from(monthDate);
        Map<AttendanceKey, AttendanceStatus> submittedStatuses = new HashMap<>();
        for (Map.Entry<String, String> entry : allParams.entrySet()) {
            String key = entry.getKey();
            if (!key.startsWith("attendance_")) {
                continue;
            }

            String value = entry.getValue();
            String[] parts = key.split("_");
            if (parts.length != 3) {
                redirectAttributes.addFlashAttribute("error", "Attendance data could not be read. No changes were saved.");
                return "redirect:/attendances?date=" + monthDate;
            }

            try {
                Long barberId = Long.parseLong(parts[1]);
                int day = Integer.parseInt(parts[2]);
                if (day < 1 || day > month.lengthOfMonth()) {
                    throw new IllegalArgumentException("Attendance day is outside the selected month.");
                }
                Barber barber = barberService.findById(barberId);
                LocalDate attendanceDate = month.atDay(day);
                AttendanceStatus status = value == null || value.isBlank() || "CLEAR".equals(value)
                    ? null
                    : AttendanceStatus.valueOf(value);
                submittedStatuses.put(new AttendanceKey(barber, attendanceDate), status);
            } catch (RuntimeException exception) {
                redirectAttributes.addFlashAttribute("error", "Attendance data is invalid. No changes were saved.");
                return "redirect:/attendances?date=" + monthDate;
            }
        }

        int savedCount = 0;
        int clearedCount = 0;
        for (Map.Entry<AttendanceKey, AttendanceStatus> entry : submittedStatuses.entrySet()) {
            AttendanceKey key = entry.getKey();
            Attendance attendance = attendanceRepository.findByBarberAndAttendanceDate(key.barber(), key.date()).orElse(null);
            if (entry.getValue() == null) {
                String submittedValue = allParams.get("attendance_" + key.barber().getId() + "_" + key.date().getDayOfMonth());
                if (!"CLEAR".equals(submittedValue)) {
                    continue;
                }
                if (attendance != null) {
                    attendanceRepository.delete(attendance);
                    clearedCount++;
                }
                continue;
            }

            if (attendance == null) {
                attendance = new Attendance();
                attendance.setBarber(key.barber());
                attendance.setAttendanceDate(key.date());
            }
            attendance.setStatus(entry.getValue());
            attendanceRepository.save(attendance);
            savedCount++;
        }

        redirectAttributes.addFlashAttribute("message",
                "Attendance saved: " + savedCount + " marked, " + clearedCount + " cleared.");
        return "redirect:/attendances?date=" + monthDate;
    }

    private record AttendanceKey(Barber barber, LocalDate date) {
    }

    @PostMapping
    public String save(@RequestParam Long barberId,
                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate attendanceDate,
                       @RequestParam AttendanceStatus status,
                       @RequestParam(required = false) String remarks,
                       RedirectAttributes redirectAttributes) {
        Barber barber = barberService.findById(barberId);
        Attendance attendance = attendanceRepository.findByBarberAndAttendanceDate(barber, attendanceDate)
                .orElse(new Attendance());

        attendance.setBarber(barber);
        attendance.setAttendanceDate(attendanceDate);
        attendance.setStatus(status);
        attendance.setRemarks(remarks);
        attendanceRepository.save(attendance);

        redirectAttributes.addFlashAttribute("message", "Attendance saved successfully.");
        return "redirect:/attendances?date=" + attendanceDate;
    }

    public record AttendanceSummary(
            Barber barber,
            YearMonth month,
            BigDecimal salary,
            long fullDays,
            long halfDays,
            long leaveDays,
            long weeklyOffDays) {
    }
}
