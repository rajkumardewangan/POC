package com.example.salon.controller;

import com.example.salon.entity.SalonService;
import com.example.salon.service.SalonServiceService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/services")
public class SalonServiceController {

    private final SalonServiceService salonServiceService;

    public SalonServiceController(SalonServiceService salonServiceService) {
        this.salonServiceService = salonServiceService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("services", salonServiceService.findAll());
        return "services/list";
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
        salonServiceService.deleteById(id);
        redirectAttributes.addFlashAttribute("message", "Service deleted.");
        return "redirect:/services";
    }
}
