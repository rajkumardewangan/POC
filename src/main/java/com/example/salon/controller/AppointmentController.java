package com.example.salon.controller;

import com.example.salon.entity.Appointment;
import com.example.salon.entity.AppointmentStatus;
import com.example.salon.entity.Barber;
import com.example.salon.entity.Customer;
import com.example.salon.entity.SalonService;
import com.example.salon.entity.ServiceGender;
import com.example.salon.service.AppointmentService;
import com.example.salon.service.BarberService;
import com.example.salon.service.CustomerService;
import com.example.salon.service.SalonServiceService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/appointments")
public class AppointmentController {

    private static final LocalTime OPENING_TIME = LocalTime.of(9, 0);
    private static final LocalTime CLOSING_TIME = LocalTime.of(20, 0);
    private static final int SLOT_MINUTES = 30;

    private final AppointmentService appointmentService;
    private final CustomerService customerService;
    private final BarberService barberService;
    private final SalonServiceService salonServiceService;

    public AppointmentController(AppointmentService appointmentService,
                                  CustomerService customerService,
                                  BarberService barberService,
                                  SalonServiceService salonServiceService) {
        this.appointmentService = appointmentService;
        this.customerService = customerService;
        this.barberService = barberService;
        this.salonServiceService = salonServiceService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) Long barberId,
                       @RequestParam(required = false) String customerName,
                       @RequestParam(required = false) AppointmentStatus status,
                       @RequestParam(required = false)
                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                       Model model) {
        LocalDate selectedDate = date != null ? date : LocalDate.now();
        model.addAttribute("appointments", appointmentService.findFilteredAppointments(barberId, customerName, selectedDate, status));
        model.addAttribute("barbers", barberService.findAll());
        model.addAttribute("selectedBarberId", barberId);
        model.addAttribute("customerNameFilter", customerName);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedDate", selectedDate);
        return "appointments/list";
    }

    @GetMapping("/calendar")
    public String calendar(@RequestParam(required = false)
                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                            Model model) {
        LocalDate selectedDate = date != null ? date : LocalDate.now();
        List<Barber> barbers = barberService.findAll();
        List<Appointment> appointments = appointmentService.findByDate(selectedDate);

        List<LocalTime> timeSlots = buildTimeSlots();
        List<List<CalendarCell>> grid = buildGrid(barbers, appointments, timeSlots);

        model.addAttribute("barbers", barbers);
        model.addAttribute("timeSlots", timeSlots);
        model.addAttribute("grid", grid);
        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("prevDate", selectedDate.minusDays(1));
        model.addAttribute("nextDate", selectedDate.plusDays(1));
        return "appointments/calendar";
    }

    private List<LocalTime> buildTimeSlots() {
        List<LocalTime> slots = new ArrayList<>();
        LocalTime time = OPENING_TIME;
        while (time.isBefore(CLOSING_TIME)) {
            slots.add(time);
            time = time.plusMinutes(SLOT_MINUTES);
        }
        return slots;
    }

    private List<List<CalendarCell>> buildGrid(List<Barber> barbers,
                                                List<Appointment> appointments,
                                                List<LocalTime> timeSlots) {
        Map<Long, Integer> barberIndexById = new HashMap<>();
        for (int i = 0; i < barbers.size(); i++) {
            barberIndexById.put(barbers.get(i).getId(), i);
        }

        List<List<CalendarCell>> grid = new ArrayList<>();
        for (int r = 0; r < timeSlots.size(); r++) {
            List<CalendarCell> row = new ArrayList<>();
            for (int c = 0; c < barbers.size(); c++) {
                row.add(new CalendarCell(CalendarCell.Type.EMPTY, null, 1));
            }
            grid.add(row);
        }

        for (Appointment appointment : appointments) {
            Integer barberIndex = barberIndexById.get(appointment.getBarber().getId());
            if (barberIndex == null) {
                continue;
            }
            LocalTime start = appointment.getAppointmentDateTime().toLocalTime();
            int startSlot = (int) ChronoUnit.MINUTES.between(OPENING_TIME, start) / SLOT_MINUTES;
            if (startSlot < 0 || startSlot >= timeSlots.size()) {
                continue;
            }
            int span = Math.max(1, (int) Math.ceil(appointment.getEffectiveDurationMinutes() / (double) SLOT_MINUTES));
            span = Math.min(span, timeSlots.size() - startSlot);

            grid.get(startSlot).set(barberIndex, new CalendarCell(CalendarCell.Type.APPOINTMENT, appointment, span));
            for (int s = 1; s < span; s++) {
                grid.get(startSlot + s).set(barberIndex, new CalendarCell(CalendarCell.Type.SKIP, null, 1));
            }
        }

        return grid;
    }

    public static class CalendarCell {
        public enum Type { EMPTY, APPOINTMENT, SKIP }

        private final Type type;
        private final Appointment appointment;
        private final int rowspan;

        public CalendarCell(Type type, Appointment appointment, int rowspan) {
            this.type = type;
            this.appointment = appointment;
            this.rowspan = rowspan;
        }

        public Type getType() {
            return type;
        }

        public Appointment getAppointment() {
            return appointment;
        }

        public int getRowspan() {
            return rowspan;
        }
    }

    @GetMapping("/new")
    public String createForm(Model model,
                            @RequestParam(required = false) Long customerId,
                            @RequestParam(required = false) String customerName,
                            @RequestParam(required = false) String mobileNumber,
                            @RequestParam(required = false) Long barberId,
                            @RequestParam(required = false) Long serviceId,
                            @RequestParam(required = false) List<Long> serviceIds,
                            @RequestParam(required = false) ServiceGender gender,
                            @RequestParam(required = false)
                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate selectedDate) {
        return createFormInternal(model, customerId, customerName, mobileNumber, barberId, serviceId, serviceIds, gender, selectedDate);
    }

    public String createForm(Model model,
                            Long customerId,
                            Long barberId,
                            Long serviceId,
                            LocalDate selectedDate) {
        return createFormInternal(model, customerId, null, null, barberId, serviceId, null, null, selectedDate);
    }

    public String createForm(Model model,
                            Long customerId,
                            Long barberId,
                            String customerName,
                            String mobileNumber,
                            Long serviceId,
                            LocalDate selectedDate) {
        return createFormInternal(model, customerId, customerName, mobileNumber, barberId, serviceId, null, null, selectedDate);
    }

    private String createFormInternal(Model model,
                                     Long customerId,
                                     String customerName,
                                     String mobileNumber,
                                     Long barberId,
                                     Long serviceId,
                                     List<Long> serviceIds,
                                     ServiceGender gender,
                                     LocalDate selectedDate) {
        addFormLookups(model);
        model.addAttribute("statuses", AppointmentStatus.values());
        model.addAttribute("selectedCustomerId", customerId);
        model.addAttribute("selectedCustomerName", customerName);
        model.addAttribute("selectedMobileNumber", mobileNumber);
        model.addAttribute("selectedBarberId", barberId);
        model.addAttribute("selectedServiceId", serviceId);
        List<Long> resolvedServiceIds = resolveServiceIds(serviceId, serviceIds);
        model.addAttribute("selectedServiceIds", resolvedServiceIds);
        model.addAttribute("selectedServiceNames", resolveServiceNames(resolvedServiceIds));
        ServiceGender selectedGender = gender != null ? gender : resolveSelectedGender(resolvedServiceIds);
        model.addAttribute("selectedGender", selectedGender);
        model.addAttribute("genders", ServiceGender.values());
        LocalDate date = selectedDate != null ? selectedDate : LocalDate.now();

        if (selectedDate == null && barberId != null && !resolvedServiceIds.isEmpty()) {
            Barber barber = barberService.findById(barberId);
            int totalDuration = getTotalDurationMinutes(resolvedServiceIds);
            List<LocalTime> daySlots = appointmentService.findAvailableSlots(barber, totalDuration, date);

            if (daySlots.isEmpty() && date.equals(LocalDate.now())) {
                date = date.plusDays(1);
                model.addAttribute("infoMessage", "No future slots remain for today. Showing tomorrow instead.");
            }
        }

        model.addAttribute("selectedDate", date);

        if (mobileNumber != null && !mobileNumber.isBlank() && (customerName == null || customerName.isBlank())) {
            try {
                Customer customer = customerService.findByPhone(mobileNumber);
                model.addAttribute("selectedCustomerName", customer.getName());
            } catch (IllegalArgumentException ignored) {
                // keep typed name empty if no customer matches the mobile number
            }
        }

        if (customerId != null) {
            customerService.findById(customerId);
            model.addAttribute("selectedCustomerName", customerService.findById(customerId).getName());
        }

        if (barberId != null && !resolvedServiceIds.isEmpty()) {
            try {
                Barber barber = barberService.findById(barberId);
                List<SalonService> selectedServices = resolveServices(resolvedServiceIds);
                 int totalDuration = selectedServices.stream()
                        .filter(service -> service.getDurationMinutes() != null)
                        .mapToInt(SalonService::getDurationMinutes)
                        .sum();

                model.addAttribute("selectedBarberName", barber.getName());
                model.addAttribute("selectedServiceName", selectedServices.stream().map(SalonService::getName).collect(java.util.stream.Collectors.joining(", ")));
                List<LocalTime> slotOptions = new ArrayList<>();
                LocalTime slotStart = OPENING_TIME;
                while (!slotStart.plusMinutes(totalDuration).isAfter(CLOSING_TIME)) {
                    slotOptions.add(slotStart);
                    slotStart = slotStart.plusMinutes(SLOT_MINUTES);
                }

                model.addAttribute("slotOptions", slotOptions);
                model.addAttribute("selectedServiceDurationMinutes", totalDuration);
                model.addAttribute("availableSlots", appointmentService.findAvailableSlots(barber, totalDuration, date));
            } catch (IllegalArgumentException ex) {
                model.addAttribute("errorMessage", ex.getMessage());
                model.addAttribute("availableSlots", List.of());
                model.addAttribute("slotOptions", List.of());
            }
        } else {
            model.addAttribute("availableSlots", List.of());
            model.addAttribute("slotOptions", List.of());
        }

        return "appointments/form";
    }

    private List<Long> resolveServiceIds(Long serviceId, List<Long> serviceIds) {
        if (serviceIds != null && !serviceIds.isEmpty()) {
            return serviceIds.stream().filter(java.util.Objects::nonNull).distinct().toList();
        }
        if (serviceId != null) {
            return List.of(serviceId);
        }
        return List.of();
    }

    private ServiceGender resolveSelectedGender(List<Long> serviceIds) {
        if (serviceIds == null || serviceIds.isEmpty()) {
            return null;
        }
        return resolveServices(serviceIds).stream()
                .map(SalonService::getGender)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private int getTotalDurationMinutes(List<Long> serviceIds) {
        if (serviceIds == null || serviceIds.isEmpty()) {
            return 0;
        }
        return resolveServices(serviceIds).stream()
                .filter(service -> service.getDurationMinutes() != null)
                .mapToInt(SalonService::getDurationMinutes)
                .sum();
    }

    private String resolveServiceNames(List<Long> serviceIds) {
        if (serviceIds == null || serviceIds.isEmpty()) {
            return "";
        }
        return resolveServices(serviceIds).stream()
                .map(SalonService::getName)
                .collect(java.util.stream.Collectors.joining(", "));
    }

    private List<SalonService> resolveServices(List<Long> serviceIds) {
        return serviceIds.stream()
                .map(salonServiceService::findById)
                .toList();
    }

    @GetMapping("/lookup-customer")
    @ResponseBody
    public Map<String, String> lookupCustomerByMobile(@RequestParam String mobileNumber) {
        try {
            Customer customer = customerService.findByPhone(mobileNumber);
            return Map.of("name", customer.getName() == null ? "" : customer.getName());
        } catch (IllegalArgumentException ex) {
            return Map.of("name", "");
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Appointment appointment = appointmentService.findById(id);
        addFormLookups(model);
        model.addAttribute("appointment", appointment);
        model.addAttribute("selectedCustomerId", appointment.getCustomer().getId());
        model.addAttribute("selectedBarberId", appointment.getBarber().getId());
        model.addAttribute("selectedServiceId", appointment.getService().getId());
        model.addAttribute("selectedCustomerName", appointment.getCustomer().getName());
        model.addAttribute("selectedBarberName", appointment.getBarber().getName());
        model.addAttribute("selectedServiceName", appointment.getService().getName());
        model.addAttribute("selectedDate", appointment.getAppointmentDateTime().toLocalDate());
        model.addAttribute("selectedTime", appointment.getAppointmentDateTime().toLocalTime());
        model.addAttribute("availableSlots", List.of(appointment.getAppointmentDateTime().toLocalTime()));
        model.addAttribute("statuses", AppointmentStatus.values());
        return "appointments/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @RequestParam Long customerId,
                         @RequestParam Long barberId,
                         @RequestParam(required = false) Long serviceId,
                         @RequestParam(required = false) List<Long> serviceIds,
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime appointmentDateTime,
                         @RequestParam AppointmentStatus status,
                         RedirectAttributes redirectAttributes) {
        Appointment appointment = appointmentService.findById(id);
        appointment.setCustomer(customerService.findById(customerId));
        appointment.setBarber(barberService.findById(barberId));
        List<Long> resolvedServiceIds = resolveServiceIds(serviceId, serviceIds);
        List<SalonService> services = resolvedServiceIds.stream()
                .map(salonServiceService::findById)
                .toList();
        appointment.setServices(services);
        if (!services.isEmpty()) {
            appointment.setService(services.get(0));
        }
        appointment.setAppointmentDateTime(appointmentDateTime);
        appointment.setStatus(status);
        appointmentService.update(appointment);
        redirectAttributes.addFlashAttribute("message", "Appointment updated successfully.");
        return "redirect:/appointments";
    }

    public String save(String customerName,
                       String mobileNumber,
                       Long barberId,
                       Long serviceId,
                       LocalDateTime appointmentDateTime,
                       AppointmentStatus status,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        return save(customerName, mobileNumber, barberId, serviceId, serviceId == null ? null : List.of(serviceId),
                appointmentDateTime, status, model, redirectAttributes);
    }

    @PostMapping
    public String save(@RequestParam String customerName,
                        @RequestParam String mobileNumber,
                        @RequestParam Long barberId,
                        @RequestParam(required = false) Long serviceId,
                        @RequestParam(required = false) List<Long> serviceIds,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime appointmentDateTime,
                        @RequestParam(required = false) AppointmentStatus status,
                        Model model,
                        RedirectAttributes redirectAttributes) {
        try {
            Customer customer = customerService.findOrCreateCustomer(customerName, mobileNumber);
            List<Long> resolvedServiceIds = resolveServiceIds(serviceId, serviceIds);
            if (resolvedServiceIds.isEmpty()) {
                throw new IllegalArgumentException("Please select at least one service.");
            }

            List<SalonService> selectedServices = resolvedServiceIds.stream()
                    .map(salonServiceService::findById)
                    .toList();

            Appointment appointment = new Appointment();
            appointment.setCustomer(customer);
            appointment.setBarber(barberService.findById(barberId));
            appointment.setServices(selectedServices);
            appointment.setService(selectedServices.get(0));
            appointment.setAppointmentDateTime(appointmentDateTime);
            appointment.setStatus(status != null ? status : AppointmentStatus.SCHEDULED);

            appointmentService.save(appointment);
            redirectAttributes.addFlashAttribute("message", "Appointment booked successfully.");
            return "redirect:/appointments";
        } catch (IllegalStateException | IllegalArgumentException ex) {
            addFormLookups(model);
            model.addAttribute("statuses", AppointmentStatus.values());
            model.addAttribute("selectedCustomerName", customerName);
            model.addAttribute("selectedMobileNumber", mobileNumber);
            model.addAttribute("errorMessage", ex.getMessage());
            return "appointments/form";
        }
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id,
                              @RequestParam AppointmentStatus status,
                              RedirectAttributes redirectAttributes) {
        appointmentService.updateStatus(id, status);
        redirectAttributes.addFlashAttribute("message", "Appointment status updated to " + status + ".");
        return "redirect:/appointments";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        appointmentService.deleteById(id);
        redirectAttributes.addFlashAttribute("message", "Appointment cancelled.");
        return "redirect:/appointments";
    }

    private void addFormLookups(Model model) {
        List<SalonService> services = salonServiceService.findAll();
        model.addAttribute("customers", customerService.findAll());
        model.addAttribute("barbers", barberService.findAll());
        model.addAttribute("services", services);
        model.addAttribute("menServices", services.stream()
                .filter(service -> service.getGender() == null || service.getGender() == com.example.salon.entity.ServiceGender.MEN)
                .sorted(java.util.Comparator.comparing(SalonService::getName))
                .toList());
        model.addAttribute("womenServices", services.stream()
                .filter(service -> service.getGender() == com.example.salon.entity.ServiceGender.WOMEN)
                .sorted(java.util.Comparator.comparing(SalonService::getName))
                .toList());
    }
}
