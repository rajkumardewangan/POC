package com.example.salon.controller;

import com.example.salon.entity.Barber;
import com.example.salon.service.AppointmentService;
import com.example.salon.service.BarberService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.List;

@Controller
@RequestMapping("/barbers")
public class BarberController {

    private final BarberService barberService;
    private final AppointmentService appointmentService;

    public BarberController(BarberService barberService, AppointmentService appointmentService) {
        this.barberService = barberService;
        this.appointmentService = appointmentService;
    }

    @GetMapping
    public String list(Model model) {
        List<Barber> barbers = barberService.findAll();
        barbers.forEach(barber -> barber.setAppointmentCount((long) appointmentService.findAll().stream()
                .filter(a -> a.getBarber() != null && a.getBarber().getId().equals(barber.getId()))
                .count()));
        model.addAttribute("barbers", barbers);
        model.addAttribute("topBarber", barbers.stream()
                .max(Comparator.comparing(Barber::getAppointmentCount))
                .orElse(null));
        return "barbers/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Barber barber = barberService.findById(id);
        List<com.example.salon.entity.Appointment> appointments = appointmentService.findAll().stream()
                .filter(a -> a.getBarber() != null && a.getBarber().getId().equals(id))
                .sorted(java.util.Comparator.comparing(a -> a.getAppointmentDateTime() != null ? a.getAppointmentDateTime() : java.time.LocalDateTime.MIN))
                .toList();

        java.math.BigDecimal totalRevenue = appointments.stream()
                .filter(a -> a.getService() != null && a.getService().getPrice() != null)
                .map(a -> a.getService().getPrice())
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        model.addAttribute("barber", barber);
        model.addAttribute("appointments", appointments);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("appointmentCount", (long) appointments.size());
        return "barbers/detail";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("barber", new Barber());
        return "barbers/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("barber", barberService.findById(id));
        return "barbers/form";
    }

    @PostMapping
    public String save(@Valid @ModelAttribute("barber") Barber barber,
                        BindingResult bindingResult,
                        RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "barbers/form";
        }
        barberService.save(barber);
        redirectAttributes.addFlashAttribute("message", "Barber saved successfully.");
        return "redirect:/barbers";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        barberService.deleteById(id);
        redirectAttributes.addFlashAttribute("message", "Barber deleted.");
        return "redirect:/barbers";
    }
}
