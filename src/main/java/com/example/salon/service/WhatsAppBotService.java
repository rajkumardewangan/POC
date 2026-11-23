package com.example.salon.service;

import com.example.salon.entity.Appointment;
import com.example.salon.entity.AppointmentStatus;
import com.example.salon.entity.Attendance;
import com.example.salon.entity.Barber;
import com.example.salon.entity.EmployeeWorkLog;
import com.example.salon.entity.SalonService;
import com.example.salon.repository.AppointmentRepository;
import com.example.salon.repository.AttendanceRepository;
import com.example.salon.repository.BarberRepository;
import com.example.salon.repository.CustomerRepository;
import com.example.salon.repository.EmployeeWorkLogRepository;
import com.example.salon.repository.SalonServiceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class WhatsAppBotService {

    private final AppointmentRepository appointmentRepository;
    private final BarberRepository barberRepository;
    private final SalonServiceRepository salonServiceRepository;
    private final CustomerRepository customerRepository;
    private final AttendanceRepository attendanceRepository;
    private final EmployeeWorkLogRepository workLogRepository;
    private final AttendanceService attendanceService;
    private final List<String> ownerNumbers;

    public WhatsAppBotService(AppointmentRepository appointmentRepository,
                              BarberRepository barberRepository,
                              SalonServiceRepository salonServiceRepository,
                              CustomerRepository customerRepository,
                              AttendanceRepository attendanceRepository,
                              EmployeeWorkLogRepository workLogRepository,
                              AttendanceService attendanceService,
                              @Value("${salon.whatsapp.owner-numbers:}") String ownerNumbers) {
        this.appointmentRepository = appointmentRepository;
        this.barberRepository = barberRepository;
        this.salonServiceRepository = salonServiceRepository;
        this.customerRepository = customerRepository;
        this.attendanceRepository = attendanceRepository;
        this.workLogRepository = workLogRepository;
        this.attendanceService = attendanceService;
        this.ownerNumbers = Arrays.stream(ownerNumbers.split(","))
                .map(WhatsAppBotService::normalizePhone)
                .filter(number -> !number.isBlank())
                .toList();
    }

    @Transactional
    public Optional<String> handleMessage(String sender, String message) {
        String normalizedSender = normalizePhone(sender);
        String text = message == null ? "" : message.trim();
        if (text.isBlank()) {
            return Optional.empty();
        }

        if (isOwner(normalizedSender)) {
            return Optional.of(answerOwner(text));
        }

        Barber barber = findBarber(normalizedSender).orElse(null);
        if (barber == null) {
            return Optional.empty();
        }
        return Optional.of(handleEmployeeMessage(barber, text));
    }

    private String handleEmployeeMessage(Barber barber, String text) {
        String normalizedText = text.trim();
        if (normalizedText.matches("(?i)^START\\s+\\d+$")) {
            return startAppointment(barber, Long.parseLong(normalizedText.replaceAll("(?i)^START\\s+", "")));
        }
        if (normalizedText.matches("(?i)^DONE(?:\\s+\\d+)?$")) {
            String idValue = normalizedText.replaceFirst("(?i)^DONE\\s*", "").trim();
            return finishWork(barber, idValue.isBlank() ? null : Long.parseLong(idValue));
        }
        if (normalizedText.regionMatches(true, 0, "WORKING ", 0, 8)) {
            return startUnlinkedWork(barber, normalizedText.substring(8).trim());
        }
        if (normalizedText.equalsIgnoreCase("HELP")) {
            return "Employee commands:\nSTART <appointment ID>\nWORKING <service or task>\nDONE [appointment ID]";
        }
        return "I couldn't match that update. Send START <appointment ID>, WORKING <service or task>, or DONE.";
    }

    private String startAppointment(Barber barber, Long appointmentId) {
        Appointment appointment = appointmentRepository.findByIdWithDetails(appointmentId).orElse(null);
        if (appointment == null) {
            return "Appointment " + appointmentId + " was not found.";
        }
        if (appointment.getBarber() == null || !appointment.getBarber().getId().equals(barber.getId())) {
            return "That appointment is not assigned to your staff account.";
        }
        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            return "Appointment " + appointmentId + " is currently " + appointment.getStatus() + ". Only scheduled appointments can be started.";
        }

        closeCurrentWork(barber);
        appointment.setStatus(AppointmentStatus.IN_PROGRESS);
        appointmentRepository.save(appointment);

        EmployeeWorkLog log = new EmployeeWorkLog();
        log.setBarber(barber);
        log.setAppointment(appointment);
        log.setStartedAt(LocalDateTime.now());
        workLogRepository.save(log);
        return "Started appointment " + appointmentId + " for " + appointment.getCustomer().getName()
                + ": " + appointment.getServiceSummary() + ". The dashboard is updated.";
    }

    private String startUnlinkedWork(Barber barber, String description) {
        if (description.isBlank()) {
            return "Tell me the service or task, for example: WORKING Haircut.";
        }
        closeCurrentWork(barber);

        SalonService service = salonServiceRepository.findAll().stream()
                .filter(candidate -> candidate.getName() != null && candidate.getName().equalsIgnoreCase(description))
                .findFirst()
                .orElse(null);

        EmployeeWorkLog log = new EmployeeWorkLog();
        log.setBarber(barber);
        log.setService(service);
        log.setWorkDescription(description);
        log.setStartedAt(LocalDateTime.now());
        workLogRepository.save(log);
        return "Logged your current work: " + (service == null ? description : service.getName())
                + ". The dashboard is updated. Send DONE when finished.";
    }

    private String finishWork(Barber barber, Long appointmentId) {
        List<EmployeeWorkLog> logs;
        if (appointmentId != null) {
            Appointment appointment = appointmentRepository.findByIdWithDetails(appointmentId).orElse(null);
            if (appointment == null) {
                return "Appointment " + appointmentId + " was not found.";
            }
            if (appointment.getBarber() == null || !appointment.getBarber().getId().equals(barber.getId())) {
                return "That appointment is not assigned to your staff account.";
            }
            logs = workLogRepository.findByAppointmentIdAndBarberIdAndEndedAtIsNull(appointmentId, barber.getId());
            if (appointment.getStatus() == AppointmentStatus.IN_PROGRESS) {
                appointment.setStatus(AppointmentStatus.COMPLETED);
                appointmentRepository.save(appointment);
            }
        } else {
            EmployeeWorkLog current = workLogRepository.findFirstByBarberIdAndEndedAtIsNullOrderByStartedAtDesc(barber.getId()).orElse(null);
            if (current == null) {
                return "There is no active work entry to finish.";
            }
            logs = List.of(current);
            Appointment appointment = current.getAppointment();
            if (appointment != null && appointment.getStatus() == AppointmentStatus.IN_PROGRESS) {
                appointment.setStatus(AppointmentStatus.COMPLETED);
                appointmentRepository.save(appointment);
            }
        }

        if (logs.isEmpty()) {
            return "No active work entry was found for that appointment.";
        }
        logs.forEach(log -> log.setEndedAt(LocalDateTime.now()));
        workLogRepository.saveAll(logs);
        return appointmentId == null ? "Your active work entry is complete. The dashboard is updated."
                : "Appointment " + appointmentId + " is marked completed. The dashboard is updated.";
    }

    private void closeCurrentWork(Barber barber) {
        workLogRepository.findFirstByBarberIdAndEndedAtIsNullOrderByStartedAtDesc(barber.getId())
                .ifPresent(log -> {
                    log.setEndedAt(LocalDateTime.now());
                    workLogRepository.save(log);
                });
    }

    private String answerOwner(String text) {
        String query = text.toLowerCase(Locale.ROOT);
        if (query.equals("hi") || query.equals("hello") || query.contains("help")) {
            return "Salon dashboard assistant. Ask about today's appointments, revenue, staff attendance, payroll, active work, customer count, or services.";
        }
        if (query.contains("working") || query.contains("in progress") || query.contains("active work")) {
            return currentWorkSummary();
        }
        if (query.contains("attendance") || query.contains("present") || query.contains("absent")) {
            return attendanceSummary();
        }
        if (query.contains("payroll") || query.contains("salary")) {
            return payrollSummary();
        }
        if (query.contains("revenue") || query.contains("sales") || query.contains("income")) {
            return todayRevenueSummary();
        }
        if (query.contains("customer")) {
            return "There are " + customerRepository.count() + " customers in the salon dashboard.";
        }
        if (query.contains("service")) {
            return serviceSummary();
        }
        if (query.contains("appointment") || query.contains("schedule") || query.contains("booking") || query.contains("today")) {
            return todayAppointmentSummary();
        }
        return "I can answer dashboard questions about today's appointments, revenue, staff attendance, payroll, active work, customer count, and services. Send HELP to see the list.";
    }

    private String todayAppointmentSummary() {
        LocalDate today = LocalDate.now();
        List<Appointment> appointments = appointmentRepository.findByAppointmentDateTimeBetweenOrderByAppointmentDateTime(
                today.atStartOfDay(), today.plusDays(1).atStartOfDay()).stream()
                .filter(appointment -> appointment.getStatus() != AppointmentStatus.CANCELLED)
                .toList();
        if (appointments.isEmpty()) {
            return "There are no appointments scheduled for today.";
        }
        String rows = appointments.stream().limit(12)
                .map(appointment -> appointment.getAppointmentDateTime().format(DateTimeFormatter.ofPattern("HH:mm"))
                        + " " + appointment.getCustomer().getName() + " with " + appointment.getBarber().getName()
                        + " (" + appointment.getServiceSummary() + ", " + appointment.getStatus() + ")")
                .collect(java.util.stream.Collectors.joining("\n"));
        return "Today's appointments (" + appointments.size() + "):\n" + rows;
    }

    private String todayRevenueSummary() {
        LocalDate today = LocalDate.now();
        BigDecimal revenue = appointmentRepository.findByAppointmentDateTimeBetweenOrderByAppointmentDateTime(
                        today.atStartOfDay(), today.plusDays(1).atStartOfDay()).stream()
                .filter(appointment -> appointment.getStatus() == AppointmentStatus.COMPLETED)
                .map(this::appointmentRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return "Today's completed-service revenue is ₹" + revenue.setScale(2, java.math.RoundingMode.HALF_UP) + ".";
    }

    private BigDecimal appointmentRevenue(Appointment appointment) {
        return appointment.getServices().stream()
                .filter(service -> service != null && service.getPrice() != null)
                .distinct()
                .map(SalonService::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String attendanceSummary() {
        List<Attendance> records = attendanceRepository.findByAttendanceDateBetween(LocalDate.now(), LocalDate.now());
        if (records.isEmpty()) {
            return "No attendance has been recorded for today.";
        }
        return records.stream()
                .map(record -> record.getBarber().getName() + ": " + record.getStatus())
                .collect(java.util.stream.Collectors.joining("\n", "Today's staff attendance:\n", ""));
    }

    private String payrollSummary() {
        YearMonth month = YearMonth.now();
        BigDecimal payroll = barberRepository.findAll().stream()
                .map(barber -> attendanceService.calculateMonthlySalary(barber, month,
                        attendanceRepository.findByBarberAndAttendanceDateBetween(barber, month.atDay(1), month.atEndOfMonth())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return "Estimated attendance-based payroll for " + month + " is ₹"
                + payroll.setScale(2, java.math.RoundingMode.HALF_UP) + ".";
    }

    private String currentWorkSummary() {
        List<EmployeeWorkLog> logs = workLogRepository.findByEndedAtIsNullOrderByStartedAtDesc();
        if (logs.isEmpty()) {
            return "No employees have reported active work right now.";
        }
        return logs.stream().limit(12)
                .map(log -> log.getBarber().getName() + ": " + log.getActivitySummary() + " since "
                        + log.getStartedAt().format(DateTimeFormatter.ofPattern("HH:mm")))
                .collect(java.util.stream.Collectors.joining("\n", "Current work:\n", ""));
    }

    private String serviceSummary() {
        List<SalonService> services = salonServiceRepository.findAll();
        if (services.isEmpty()) {
            return "No services are configured in the dashboard.";
        }
        return services.stream().limit(20)
                .map(service -> service.getName() + " (₹" + service.getPrice() + ", " + service.getDurationMinutes() + " min)")
                .collect(java.util.stream.Collectors.joining("\n", "Salon services:\n", ""));
    }

    private boolean isOwner(String phone) {
        return ownerNumbers.stream().anyMatch(owner -> samePhone(owner, phone));
    }

    private Optional<Barber> findBarber(String phone) {
        return barberRepository.findAll().stream()
                .filter(barber -> barber.getPhone() != null && samePhone(normalizePhone(barber.getPhone()), phone))
                .findFirst();
    }

    private static boolean samePhone(String configuredPhone, String incomingPhone) {
        if (configuredPhone.equals(incomingPhone)) {
            return true;
        }
        return configuredPhone.length() >= 10 && incomingPhone.length() >= 10
                && configuredPhone.substring(configuredPhone.length() - 10)
                .equals(incomingPhone.substring(incomingPhone.length() - 10));
    }

    private static String normalizePhone(String phone) {
        return phone == null ? "" : phone.replaceAll("\\D", "");
    }
}