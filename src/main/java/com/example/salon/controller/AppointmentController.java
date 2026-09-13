package com.example.salon.controller;

import com.example.salon.entity.Appointment;
import com.example.salon.entity.AppointmentStatus;
import com.example.salon.entity.Barber;
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
    public String list(Model model) {
        model.addAttribute("appointments", appointmentService.findAll());
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
            int span = Math.max(1, (int) Math.ceil(appointment.getService().getDurationMinutes() / (double) SLOT_MINUTES));
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
    public String createForm(Model model) {
        addFormLookups(model);
        model.addAttribute("statuses", AppointmentStatus.values());
        return "appointments/form";
    }

    @PostMapping
    public String save(@RequestParam Long customerId,
                        @RequestParam Long barberId,
                        @RequestParam Long serviceId,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime appointmentDateTime,
                        @RequestParam(required = false) AppointmentStatus status,
                        Model model,
                        RedirectAttributes redirectAttributes) {
        try {
            Appointment appointment = new Appointment();
            appointment.setCustomer(customerService.findById(customerId));
            appointment.setBarber(barberService.findById(barberId));
            appointment.setService(salonServiceService.findById(serviceId));
            appointment.setAppointmentDateTime(appointmentDateTime);
            appointment.setStatus(status != null ? status : AppointmentStatus.SCHEDULED);

            appointmentService.save(appointment);
            redirectAttributes.addFlashAttribute("message", "Appointment booked successfully.");
            return "redirect:/appointments";
        } catch (IllegalStateException | IllegalArgumentException ex) {
            addFormLookups(model);
            model.addAttribute("statuses", AppointmentStatus.values());
            model.addAttribute("errorMessage", ex.getMessage());
            return "appointments/form";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        appointmentService.deleteById(id);
        redirectAttributes.addFlashAttribute("message", "Appointment cancelled.");
        return "redirect:/appointments";
    }

    private void addFormLookups(Model model) {
        model.addAttribute("customers", customerService.findAll());
        model.addAttribute("barbers", barberService.findAll());
        model.addAttribute("services", salonServiceService.findAll());
    }
}
