package com.example.salon.controller;

import com.example.salon.entity.Appointment;
import com.example.salon.entity.Customer;
import com.example.salon.service.AppointmentService;
import com.example.salon.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/customers")
public class CustomerController {

    public static class CustomerRetentionSummary {
        private final Customer customer;
        private final long visitCount;
        private final boolean returningCustomer;

        public CustomerRetentionSummary(Customer customer, long visitCount, boolean returningCustomer) {
            this.customer = customer;
            this.visitCount = visitCount;
            this.returningCustomer = returningCustomer;
        }

        public Customer getCustomer() {
            return customer;
        }

        public long getVisitCount() {
            return visitCount;
        }

        public boolean isReturningCustomer() {
            return returningCustomer;
        }
    }

    private final CustomerService customerService;
    private final AppointmentService appointmentService;

    public CustomerController(CustomerService customerService, AppointmentService appointmentService) {
        this.customerService = customerService;
        this.appointmentService = appointmentService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String search, Model model) {
        List<Customer> customers = customerService.findAll().stream()
                .filter(customer -> search == null || search.isBlank() ||
                        customer.getName().toLowerCase().contains(search.toLowerCase()) ||
                        (customer.getPhone() != null && customer.getPhone().toLowerCase().contains(search.toLowerCase())))
                .toList();
        customers.forEach(customer -> customer.setVisitCount((long) appointmentService.findAll().stream()
                .filter(a -> a.getCustomer() != null && a.getCustomer().getId().equals(customer.getId()))
                .count()));
        model.addAttribute("customers", customers);
        model.addAttribute("topCustomer", customers.stream()
                .max(Comparator.comparing(Customer::getVisitCount))
                .orElse(null));
        model.addAttribute("search", search);
        return "customers/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Customer customer = customerService.findById(id);
        List<Appointment> appointments = appointmentService.findAll().stream()
                .filter(a -> a.getCustomer() != null && a.getCustomer().getId().equals(id))
                .sorted(Comparator.comparing(a -> a.getAppointmentDateTime() != null ? a.getAppointmentDateTime() : java.time.LocalDateTime.MIN))
                .toList();

        java.math.BigDecimal totalSpent = appointments.stream()
                .filter(a -> a.getService() != null && a.getService().getPrice() != null)
                .map(a -> a.getService().getPrice())
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        model.addAttribute("customer", customer);
        model.addAttribute("appointments", appointments);
        model.addAttribute("totalSpent", totalSpent);
        model.addAttribute("visitCount", (long) appointments.size());
        return "customers/detail";
    }

    @GetMapping("/retention")
    public String retention(Model model) {
        List<Customer> customers = customerService.findAll();
        Map<Long, Long> visitCounts = appointmentService.findAll().stream()
                .filter(a -> a.getCustomer() != null)
                .collect(Collectors.groupingBy(a -> a.getCustomer().getId(), Collectors.counting()));

        List<CustomerRetentionSummary> retentionSummaries = customers.stream()
                .map(customer -> {
                    long visitCount = visitCounts.getOrDefault(customer.getId(), 0L);
                    return new CustomerRetentionSummary(customer, visitCount, visitCount > 1);
                })
                .sorted(Comparator.comparing(CustomerRetentionSummary::getVisitCount).reversed())
                .toList();

        long totalCustomers = customers.size();
        long repeatCustomers = retentionSummaries.stream().filter(CustomerRetentionSummary::isReturningCustomer).count();
        double retentionRate = totalCustomers == 0 ? 0.0 : (repeatCustomers * 100.0) / totalCustomers;
        double averageVisits = totalCustomers == 0 ? 0.0 : retentionSummaries.stream()
                .mapToLong(CustomerRetentionSummary::getVisitCount)
                .average()
                .orElse(0.0);

        model.addAttribute("retentionSummaries", retentionSummaries);
        model.addAttribute("totalCustomers", totalCustomers);
        model.addAttribute("repeatCustomers", repeatCustomers);
        model.addAttribute("retentionRate", retentionRate);
        model.addAttribute("averageVisits", averageVisits);
        return "customers/retention";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("customer", new Customer());
        return "customers/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("customer", customerService.findById(id));
        return "customers/form";
    }

    @PostMapping
    public String save(@Valid @ModelAttribute("customer") Customer customer,
                        BindingResult bindingResult,
                        RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "customers/form";
        }
        customerService.save(customer);
        redirectAttributes.addFlashAttribute("message", "Customer saved successfully.");
        return "redirect:/customers";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        customerService.deleteById(id);
        redirectAttributes.addFlashAttribute("message", "Customer deleted.");
        return "redirect:/customers";
    }
}
