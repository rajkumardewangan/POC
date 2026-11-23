package com.example.salon.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "appointments")
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Customer is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @NotNull(message = "Barber is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "barber_id", nullable = false)
    private Barber barber;

    @NotNull(message = "Service is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private SalonService service;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "appointment_services",
            joinColumns = @JoinColumn(name = "appointment_id"),
            inverseJoinColumns = @JoinColumn(name = "service_id")
    )
    private List<SalonService> services = new ArrayList<>();

    @NotNull(message = "Date and time is required")
    @Column(nullable = false)
    private LocalDateTime appointmentDateTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status = AppointmentStatus.SCHEDULED;

    public Appointment() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public Barber getBarber() {
        return barber;
    }

    public void setBarber(Barber barber) {
        this.barber = barber;
    }

    public SalonService getService() {
        return service;
    }

    public void setService(SalonService service) {
        this.service = service;
        if (service != null && (this.services == null || this.services.isEmpty())) {
            this.services = new ArrayList<>();
            this.services.add(service);
        }
    }

    public List<SalonService> getServices() {
        if (services == null) {
            services = new ArrayList<>();
        }
        if (service != null && services.stream().noneMatch(s -> sameService(s, service))) {
            services.add(service);
        }
        return services;
    }

    private boolean sameService(SalonService left, SalonService right) {
        if (left == right) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return left.getId() != null && left.getId().equals(right.getId());
    }

    public void setServices(List<SalonService> services) {
        this.services = services == null ? new ArrayList<>() : services;
        if (this.services != null && !this.services.isEmpty()) {
            this.service = this.services.get(0);
        }
    }

    public int getEffectiveDurationMinutes() {
        List<SalonService> selectedServices = getServices();
        if (selectedServices == null || selectedServices.isEmpty()) {
            return service != null && service.getDurationMinutes() != null ? service.getDurationMinutes() : 0;
        }

        return selectedServices.stream()
                .filter(s -> s != null && s.getDurationMinutes() != null)
                .mapToInt(SalonService::getDurationMinutes)
                .sum();
    }

    public String getServiceSummary() {
        List<SalonService> selectedServices = getServices();
        if (selectedServices == null || selectedServices.isEmpty()) {
            return service != null ? service.getName() : "";
        }
        return selectedServices.stream()
                .filter(s -> s != null && s.getName() != null)
                .map(SalonService::getName)
                .distinct()
                .collect(java.util.stream.Collectors.joining(", "));
    }

    public LocalDateTime getAppointmentDateTime() {
        return appointmentDateTime;
    }

    public void setAppointmentDateTime(LocalDateTime appointmentDateTime) {
        this.appointmentDateTime = appointmentDateTime;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public void setStatus(AppointmentStatus status) {
        this.status = status;
    }
}
