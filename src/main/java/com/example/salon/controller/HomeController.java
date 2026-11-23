package com.example.salon.controller;

import com.example.salon.entity.AppUser;
import com.example.salon.entity.Appointment;
import com.example.salon.entity.AppointmentStatus;
import com.example.salon.entity.Attendance;
import com.example.salon.entity.AttendanceStatus;
import com.example.salon.entity.Barber;
import com.example.salon.entity.SalonService;
import com.example.salon.entity.UserRole;
import com.example.salon.repository.AppointmentRepository;
import com.example.salon.repository.AttendanceRepository;
import com.example.salon.repository.BarberRepository;
import com.example.salon.repository.CustomerRepository;
import com.example.salon.repository.EmployeeWorkLogRepository;
import com.example.salon.repository.SalonServiceRepository;
import com.example.salon.service.AttendanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Controller
public class HomeController {

    private final CustomerRepository customerRepository;
    private final BarberRepository barberRepository;
    private final SalonServiceRepository salonServiceRepository;
    private final AppointmentRepository appointmentRepository;
    private final AttendanceRepository attendanceRepository;
    private final AttendanceService attendanceService;
    private final EmployeeWorkLogRepository workLogRepository;

    @Autowired
    public HomeController(CustomerRepository customerRepository,
                           BarberRepository barberRepository,
                           SalonServiceRepository salonServiceRepository,
                           AppointmentRepository appointmentRepository,
                           AttendanceRepository attendanceRepository,
                           AttendanceService attendanceService,
                           EmployeeWorkLogRepository workLogRepository) {
        this.customerRepository = customerRepository;
        this.barberRepository = barberRepository;
        this.salonServiceRepository = salonServiceRepository;
        this.appointmentRepository = appointmentRepository;
        this.attendanceRepository = attendanceRepository;
        this.attendanceService = attendanceService;
        this.workLogRepository = workLogRepository;
    }

    public HomeController(CustomerRepository customerRepository,
                          BarberRepository barberRepository,
                          SalonServiceRepository salonServiceRepository,
                          AppointmentRepository appointmentRepository,
                          AttendanceRepository attendanceRepository,
                          AttendanceService attendanceService) {
        this(customerRepository, barberRepository, salonServiceRepository, appointmentRepository,
                attendanceRepository, attendanceService, null);
    }

    public HomeController(CustomerRepository customerRepository,
                          BarberRepository barberRepository,
                          SalonServiceRepository salonServiceRepository,
                          AppointmentRepository appointmentRepository) {
        this(customerRepository, barberRepository, salonServiceRepository, appointmentRepository,
                null, null, null);
    }

    public static class DailyRevenueSummary {
        private final LocalDate date;
        private final BigDecimal revenue;
        private final long appointments;

        public DailyRevenueSummary(LocalDate date, BigDecimal revenue, long appointments) {
            this.date = date;
            this.revenue = revenue;
            this.appointments = appointments;
        }

        public LocalDate getDate() {
            return date;
        }

        public BigDecimal getRevenue() {
            return revenue;
        }

        public long getAppointments() {
            return appointments;
        }
    }

    public static class WeeklyRevenueSummary {
        private final LocalDate startDate;
        private final LocalDate endDate;
        private final BigDecimal revenue;
        private final long appointments;

        public WeeklyRevenueSummary(LocalDate startDate, LocalDate endDate, BigDecimal revenue, long appointments) {
            this.startDate = startDate;
            this.endDate = endDate;
            this.revenue = revenue;
            this.appointments = appointments;
        }

        public LocalDate getStartDate() {
            return startDate;
        }

        public LocalDate getEndDate() {
            return endDate;
        }

        public BigDecimal getRevenue() {
            return revenue;
        }

        public long getAppointments() {
            return appointments;
        }
    }

    public static class MonthlyRevenueSummary {
        private final LocalDate monthStart;
        private final LocalDate monthEnd;
        private final BigDecimal revenue;
        private final long appointments;

        public MonthlyRevenueSummary(LocalDate monthStart, LocalDate monthEnd, BigDecimal revenue, long appointments) {
            this.monthStart = monthStart;
            this.monthEnd = monthEnd;
            this.revenue = revenue;
            this.appointments = appointments;
        }

        public LocalDate getMonthStart() {
            return monthStart;
        }

        public LocalDate getMonthEnd() {
            return monthEnd;
        }

        public BigDecimal getRevenue() {
            return revenue;
        }

        public long getAppointments() {
            return appointments;
        }
    }

    public static class ServicePreferenceRow {
        private final String customerName;
        private final String phone;
        private final String serviceName;
        private final int visitCount;
        private final LocalDate lastBookedDate;

        public ServicePreferenceRow(String customerName, String phone, String serviceName, int visitCount, LocalDate lastBookedDate) {
            this.customerName = customerName;
            this.phone = phone;
            this.serviceName = serviceName;
            this.visitCount = visitCount;
            this.lastBookedDate = lastBookedDate;
        }

        public String getCustomerName() {
            return customerName;
        }

        public String getPhone() {
            return phone;
        }

        public String getServiceName() {
            return serviceName;
        }

        public int getVisitCount() {
            return visitCount;
        }

        public LocalDate getLastBookedDate() {
            return lastBookedDate;
        }
    }

    public static class PayrollSummaryRow {
        private final Barber barber;
        private final YearMonth month;
        private final long fullDays;
        private final long halfDays;
        private final long leaveDays;
        private final BigDecimal baseSalary;
        private final BigDecimal grossSalary;
        private final BigDecimal allowances;
        private final BigDecimal fixedDeduction;
        private final BigDecimal netSalary;

        public PayrollSummaryRow(Barber barber, YearMonth month, long fullDays, long halfDays, long leaveDays,
                     BigDecimal baseSalary, BigDecimal grossSalary, BigDecimal allowances,
                     BigDecimal fixedDeduction, BigDecimal netSalary) {
            this.barber = barber;
            this.month = month;
            this.fullDays = fullDays;
            this.halfDays = halfDays;
            this.leaveDays = leaveDays;
            this.baseSalary = baseSalary;
            this.grossSalary = grossSalary;
            this.allowances = allowances;
            this.fixedDeduction = fixedDeduction;
            this.netSalary = netSalary;
        }

        public Barber getBarber() {
            return barber;
        }

        public YearMonth getMonth() {
            return month;
        }

        public long getFullDays() {
            return fullDays;
        }

        public long getHalfDays() {
            return halfDays;
        }

        public long getLeaveDays() {
            return leaveDays;
        }

        public BigDecimal getBaseSalary() {
            return baseSalary;
        }

        public BigDecimal getGrossSalary() {
            return grossSalary;
        }

        public BigDecimal getAllowances() {
            return allowances;
        }

        public BigDecimal getFixedDeduction() {
            return fixedDeduction;
        }

        public BigDecimal getNetSalary() {
            return netSalary;
        }
    }

    @GetMapping("/")
    public String home(Model model) {
        AppUser currentUser = (AppUser) model.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }
        if (currentUser.getRole() == UserRole.REGULAR) {
            return "redirect:/appointments/calendar";
        }

        List<Appointment> appointments = appointmentRepository.findAllByOrderByAppointmentDateTimeDesc();
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        List<Appointment> todaysAppointments = appointments.stream()
                .filter(a -> a.getAppointmentDateTime() != null && a.getAppointmentDateTime().toLocalDate().equals(today))
                .filter(a -> a.getStatus() != AppointmentStatus.CANCELLED)
                .sorted(Comparator.comparing(Appointment::getAppointmentDateTime))
                .toList();

        long scheduledCount = todaysAppointments.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.SCHEDULED)
                .count();
        long completedCount = todaysAppointments.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.COMPLETED)
                .count();

        BigDecimal totalRevenue = todaysAppointments.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.COMPLETED)
                .map(Appointment::getService)
                .filter(service -> service != null && service.getPrice() != null)
                .map(SalonService::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Appointment> todayAppointments = todaysAppointments.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.SCHEDULED
                    || a.getStatus() == AppointmentStatus.IN_PROGRESS
                    || a.getStatus() == AppointmentStatus.COMPLETED)
                .toList();

        List<Appointment> upcomingAppointments = todaysAppointments.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.SCHEDULED)
                .filter(a -> a.getAppointmentDateTime().toLocalTime().compareTo(now) >= 0)
                .sorted(Comparator.comparing(Appointment::getAppointmentDateTime))
                .limit(5)
                .toList();

        String topBarberName = todaysAppointments.stream()
                .filter(a -> a.getBarber() != null)
                .collect(Collectors.groupingBy(a -> a.getBarber().getName(), Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");

        String topServiceName = todaysAppointments.stream()
                .filter(a -> a.getService() != null)
                .collect(Collectors.groupingBy(a -> a.getService().getName(), Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");

        YearMonth currentMonth = YearMonth.now();
        BigDecimal monthlyPayroll = barberRepository.findAll().stream()
                .map(barber -> {
                    List<Attendance> monthRecords = attendanceRepository.findByBarberAndAttendanceDateBetween(
                            barber,
                            currentMonth.atDay(1),
                            currentMonth.atEndOfMonth());
                    return attendanceService.calculateMonthlySalary(barber, currentMonth, monthRecords);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("customerCount", customerRepository.count());
        model.addAttribute("barberCount", barberRepository.count());
        model.addAttribute("serviceCount", salonServiceRepository.count());
        model.addAttribute("appointmentCount", todaysAppointments.size());
        model.addAttribute("scheduledCount", scheduledCount);
        model.addAttribute("completedCount", completedCount);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("monthlyPayroll", monthlyPayroll);
        model.addAttribute("topBarberName", topBarberName);
        model.addAttribute("topServiceName", topServiceName);
        model.addAttribute("todayAppointments", todayAppointments);
        model.addAttribute("upcomingAppointments", upcomingAppointments);
        model.addAttribute("activeWorkLogs", workLogRepository == null
            ? List.of() : workLogRepository.findByEndedAtIsNullOrderByStartedAtDesc());
        return "index";
    }

    @GetMapping("/reports")
    public String reportsOverview(Model model) {
        model.addAttribute("reportLinks", List.of(
                new ReportLink("Daily Report", "/reports/daily", "Revenue and appointment totals by day."),
                new ReportLink("Weekly Report", "/reports/weekly", "Performance trends across the week."),
                new ReportLink("Monthly Report", "/reports/monthly", "Revenue and appointment summary by month."),
                new ReportLink("Payroll Report", "/reports/payroll", "Attendance-based salary summary for each barber."),
                new ReportLink("Service Preferences", "/reports/service-preferences", "Customers grouped by preferred service.")
        ));
        return "reports/index";
    }

    public static class ReportLink {
        private final String title;
        private final String url;
        private final String description;

        public ReportLink(String title, String url, String description) {
            this.title = title;
            this.url = url;
            this.description = description;
        }

        public String getTitle() {
            return title;
        }

        public String getUrl() {
            return url;
        }

        public String getDescription() {
            return description;
        }
    }

    @GetMapping("/reports/payroll")
    public String payrollReport(Model model) {
        YearMonth month = YearMonth.now();
        List<Barber> barbers = barberRepository.findAll().stream()
                .sorted(Comparator.comparing(Barber::getName))
                .toList();

        List<PayrollSummaryRow> payrollRows = barbers.stream()
                .map(barber -> {
                    List<Attendance> records = attendanceRepository.findByBarberAndAttendanceDateBetween(
                            barber,
                            month.atDay(1),
                            month.atEndOfMonth());
                    BigDecimal netSalary = attendanceService.calculateMonthlySalary(barber, month, records);
                    long fullDays = records.stream().filter(a -> a.getStatus() == AttendanceStatus.FULL_DAY).count();
                    long halfDays = records.stream().filter(a -> a.getStatus() == AttendanceStatus.HALF_DAY).count();
                    long leaveDays = records.stream().filter(a -> a.getStatus() == AttendanceStatus.LEAVE).count();
                        BigDecimal allowances = barber.getHousingAllowance()
                            .add(barber.getTransportAllowance())
                            .add(barber.getOtherAllowance());
                        return new PayrollSummaryRow(barber, month, fullDays, halfDays, leaveDays,
                            barber.getBaseSalary(), barber.getGrossMonthlySalary(), allowances,
                            barber.getFixedSalaryDeduction(), netSalary);
                })
                .toList();

        BigDecimal totalPayroll = payrollRows.stream()
                .map(PayrollSummaryRow::getNetSalary)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("month", month);
        model.addAttribute("payrollRows", payrollRows);
        model.addAttribute("totalPayroll", totalPayroll);
        return "reports/payroll";
    }

    @GetMapping("/reports/daily")
    public String dailyReport(Model model) {
        List<Appointment> appointments = appointmentRepository.findAllByOrderByAppointmentDateTimeDesc();

        List<DailyRevenueSummary> dailyReports = appointments.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.COMPLETED)
                .filter(a -> a.getAppointmentDateTime() != null)
                .collect(Collectors.groupingBy(a -> a.getAppointmentDateTime().toLocalDate()))
                .entrySet().stream()
                .map(entry -> {
                    BigDecimal revenue = entry.getValue().stream()
                            .map(Appointment::getService)
                            .filter(Objects::nonNull)
                            .map(SalonService::getPrice)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new DailyRevenueSummary(entry.getKey(), revenue, entry.getValue().size());
                })
                .sorted(Comparator.comparing(DailyRevenueSummary::getDate).reversed())
                .toList();

        BigDecimal totalRevenue = dailyReports.stream()
                .map(DailyRevenueSummary::getRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("dailyReports", dailyReports);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("reportDate", LocalDate.now());
        return "reports/daily";
    }

    @GetMapping("/reports/weekly")
    public String weeklyReport(Model model) {
        List<Appointment> appointments = appointmentRepository.findAllByOrderByAppointmentDateTimeDesc();

        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.minusDays(today.getDayOfWeek().getValue() - 1L);
        LocalDate endOfWeek = startOfWeek.plusDays(6);

        List<WeeklyRevenueSummary> weeklyReports = appointments.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.COMPLETED)
                .filter(a -> a.getAppointmentDateTime() != null)
                .collect(Collectors.groupingBy(a -> {
                    LocalDate appointmentDate = a.getAppointmentDateTime().toLocalDate();
                    LocalDate weekStart = appointmentDate.minusDays(appointmentDate.getDayOfWeek().getValue() - 1L);
                    return weekStart;
                }))
                .entrySet().stream()
                .map(entry -> {
                    BigDecimal revenue = entry.getValue().stream()
                            .map(Appointment::getService)
                            .filter(Objects::nonNull)
                            .map(SalonService::getPrice)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    LocalDate weekStart = entry.getKey();
                    LocalDate weekEnd = weekStart.plusDays(6);
                    return new WeeklyRevenueSummary(weekStart, weekEnd, revenue, entry.getValue().size());
                })
                .sorted(Comparator.comparing(WeeklyRevenueSummary::getStartDate).reversed())
                .toList();

        BigDecimal totalRevenue = weeklyReports.stream()
                .map(WeeklyRevenueSummary::getRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("weeklyReports", weeklyReports);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("weekStart", startOfWeek);
        model.addAttribute("weekEnd", endOfWeek);
        return "reports/weekly";
    }

    @GetMapping("/reports/monthly")
    public String monthlyReport(Model model) {
        List<Appointment> appointments = appointmentRepository.findAllByOrderByAppointmentDateTimeDesc();

        List<MonthlyRevenueSummary> monthlyReports = appointments.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.COMPLETED)
                .filter(a -> a.getAppointmentDateTime() != null)
                .collect(Collectors.groupingBy(a -> a.getAppointmentDateTime().toLocalDate().withDayOfMonth(1)))
                .entrySet().stream()
                .map(entry -> {
                    BigDecimal revenue = entry.getValue().stream()
                            .map(Appointment::getService)
                            .filter(Objects::nonNull)
                            .map(SalonService::getPrice)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    LocalDate monthStart = entry.getKey();
                    LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());
                    return new MonthlyRevenueSummary(monthStart, monthEnd, revenue, entry.getValue().size());
                })
                .sorted(Comparator.comparing(MonthlyRevenueSummary::getMonthStart).reversed())
                .toList();

        BigDecimal totalRevenue = monthlyReports.stream()
                .map(MonthlyRevenueSummary::getRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDate currentMonth = LocalDate.now().withDayOfMonth(1);
        model.addAttribute("monthlyReports", monthlyReports);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("currentMonth", currentMonth);
        return "reports/monthly";
    }

    @GetMapping("/reports/service-preferences")
    public String servicePreferenceReport(@RequestParam(required = false) Long serviceId, Model model) {
        List<Appointment> appointments = appointmentRepository.findAllByOrderByAppointmentDateTimeDesc();
        List<SalonService> services = salonServiceRepository.findAll();

        List<ServicePreferenceRow> rows = appointments.stream()
                .filter(a -> a.getCustomer() != null && a.getService() != null)
                .filter(a -> a.getStatus() != AppointmentStatus.CANCELLED)
                .filter(a -> serviceId == null || a.getService().getId().equals(serviceId))
                .collect(Collectors.groupingBy(a -> a.getCustomer().getId() + "|" + a.getService().getId()))
                .values().stream()
                .map(group -> {
                    Appointment first = group.get(0);
                    String customerName = first.getCustomer().getName();
                    String phone = first.getCustomer().getPhone();
                    String serviceName = first.getService().getName();
                    int visitCount = group.size();
                    LocalDate lastBookedDate = group.stream()
                            .map(a -> a.getAppointmentDateTime() != null ? a.getAppointmentDateTime().toLocalDate() : null)
                            .filter(Objects::nonNull)
                            .max(LocalDate::compareTo)
                            .orElse(null);
                    return new ServicePreferenceRow(customerName, phone, serviceName, visitCount, lastBookedDate);
                })
                .sorted(Comparator.comparing(ServicePreferenceRow::getServiceName).thenComparing(ServicePreferenceRow::getCustomerName))
                .toList();

        model.addAttribute("services", services);
        model.addAttribute("selectedServiceId", serviceId);
        model.addAttribute("selectedServiceName", serviceId != null ? services.stream()
                .filter(s -> s.getId().equals(serviceId))
                .map(SalonService::getName)
                .findFirst()
                .orElse("All Services") : "All Services");
        model.addAttribute("rows", rows);
        model.addAttribute("customerCount", rows.stream().map(ServicePreferenceRow::getCustomerName).distinct().count());
        return "reports/service-preferences";
    }
}
