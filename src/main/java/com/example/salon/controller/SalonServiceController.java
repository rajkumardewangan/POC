package com.example.salon.controller;

import com.example.salon.entity.Appointment;
import com.example.salon.entity.SalonService;
import com.example.salon.service.AppointmentService;
import com.example.salon.service.SalonServiceService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Controller
@RequestMapping("/services")
public class SalonServiceController {

    private final SalonServiceService salonServiceService;
    private final AppointmentService appointmentService;

    public SalonServiceController(SalonServiceService salonServiceService, AppointmentService appointmentService) {
        this.salonServiceService = salonServiceService;
        this.appointmentService = appointmentService;
    }

    @GetMapping
    public String list(Model model) {
        List<SalonService> services = salonServiceService.findAll();
        services.forEach(service -> {
            BigDecimal revenue = appointmentService.findAll().stream()
                    .filter(a -> a.getService() != null && a.getService().getId().equals(service.getId()))
                    .map(a -> a.getService().getPrice())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            service.setRevenue(revenue);
        });

        List<SalonService> menServices = services.stream()
                .filter(service -> service.getGender() == null || service.getGender() == com.example.salon.entity.ServiceGender.MEN)
                .sorted(Comparator.comparing(SalonService::getName))
                .toList();

        List<SalonService> womenServices = services.stream()
                .filter(service -> service.getGender() == com.example.salon.entity.ServiceGender.WOMEN)
                .sorted(Comparator.comparing(SalonService::getName))
                .toList();

        model.addAttribute("menServices", menServices);
        model.addAttribute("womenServices", womenServices);
        model.addAttribute("topService", services.stream()
                .max(Comparator.comparing(SalonService::getRevenue))
                .orElse(null));
        return "services/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        SalonService service = salonServiceService.findById(id);
        List<Appointment> appointments = appointmentService.findAll().stream()
                .filter(a -> a.getService() != null && a.getService().getId().equals(id))
                .sorted(java.util.Comparator.comparing(a -> a.getAppointmentDateTime() != null ? a.getAppointmentDateTime() : java.time.LocalDateTime.MIN))
                .toList();

        java.math.BigDecimal totalRevenue = appointments.stream()
                .filter(a -> a.getService() != null && a.getService().getPrice() != null)
                .map(a -> a.getService().getPrice())
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        model.addAttribute("service", service);
        model.addAttribute("appointments", appointments);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("appointmentCount", (long) appointments.size());
        return "services/detail";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("service", new SalonService());
        return "services/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("service", salonServiceService.findById(id));
        return "services/form";
    }

    @PostMapping
    public String save(@Valid @ModelAttribute("service") SalonService service,
                        BindingResult bindingResult,
                        RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "services/form";
        }
        salonServiceService.save(service);
        redirectAttributes.addFlashAttribute("message", "Service saved successfully.");
        return "redirect:/services";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            salonServiceService.deleteById(id);
            redirectAttributes.addFlashAttribute("message", "Service deleted.");
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/services";
    }
}
