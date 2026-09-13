package com.example.salon.controller;

import com.example.salon.repository.AppointmentRepository;
import com.example.salon.repository.BarberRepository;
import com.example.salon.repository.CustomerRepository;
import com.example.salon.repository.SalonServiceRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final CustomerRepository customerRepository;
    private final BarberRepository barberRepository;
    private final SalonServiceRepository salonServiceRepository;
    private final AppointmentRepository appointmentRepository;

    public HomeController(CustomerRepository customerRepository,
                           BarberRepository barberRepository,
                           SalonServiceRepository salonServiceRepository,
                           AppointmentRepository appointmentRepository) {
        this.customerRepository = customerRepository;
        this.barberRepository = barberRepository;
        this.salonServiceRepository = salonServiceRepository;
        this.appointmentRepository = appointmentRepository;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("customerCount", customerRepository.count());
        model.addAttribute("barberCount", barberRepository.count());
        model.addAttribute("serviceCount", salonServiceRepository.count());
        model.addAttribute("appointmentCount", appointmentRepository.count());
        return "index";
    }
}
