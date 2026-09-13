package com.example.salon.controller;

import com.example.salon.entity.Barber;
import com.example.salon.service.BarberService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/barbers")
public class BarberController {

    private final BarberService barberService;

    public BarberController(BarberService barberService) {
        this.barberService = barberService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("barbers", barberService.findAll());
        return "barbers/list";
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
