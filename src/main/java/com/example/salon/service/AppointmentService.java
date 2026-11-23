package com.example.salon.service;

import com.example.salon.entity.Appointment;
import com.example.salon.entity.AppointmentStatus;
import com.example.salon.entity.Barber;
import com.example.salon.entity.SalonService;
import com.example.salon.repository.AppointmentRepository;
import org.hibernate.LazyInitializationException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AppointmentService {

    public static final LocalTime OPENING_TIME = LocalTime.of(9, 0);
    public static final LocalTime CLOSING_TIME = LocalTime.of(20, 0);
    public static final int SLOT_MINUTES = 30;

    private final AppointmentRepository appointmentRepository;

    public AppointmentService(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    public List<Appointment> findAll() {
        return appointmentRepository.findAllByOrderByAppointmentDateTimeDesc();
    }

    public List<Appointment> findByDate(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        return appointmentRepository.findByAppointmentDateTimeBetweenOrderByAppointmentDateTime(start, end);
    }

    public List<Appointment> findFilteredAppointments(Long barberId, String customerName, LocalDate date) {
        return findFilteredAppointments(barberId, customerName, date, null);
    }

    public List<Appointment> findFilteredAppointments(Long barberId, String customerName, LocalDate date, AppointmentStatus status) {
        String normalizedName = (customerName == null || customerName.isBlank()) ? null : customerName.trim();

        if (date == null) {
            return appointmentRepository.findAllByOrderByAppointmentDateTimeDesc().stream()
                    .filter(a -> (barberId == null || a.getBarber() != null && a.getBarber().getId().equals(barberId)))
                    .filter(a -> (normalizedName == null || a.getCustomer() != null && a.getCustomer().getName() != null && a.getCustomer().getName().toLowerCase().contains(normalizedName.toLowerCase())))
                    .filter(a -> (status == null || a.getStatus() == status))
                    .toList();
        }

        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        return appointmentRepository.findFilteredAppointments(barberId, normalizedName, status, start, end);
    }

    public List<LocalTime> findAvailableSlots(Barber barber, SalonService service, LocalDate date) {
        return findAvailableSlots(barber, service == null || service.getDurationMinutes() == null ? 0 : service.getDurationMinutes(), date);
    }

    public List<LocalTime> findAvailableSlots(Barber barber, int totalDurationMinutes, LocalDate date) {
        if (barber == null || date == null || totalDurationMinutes <= 0) {
            return List.of();
        }

        LocalDateTime now = LocalDateTime.now();
        List<LocalTime> availableSlots = new ArrayList<>();
        List<Appointment> existingAppointments = appointmentRepository.findByBarberAndAppointmentDateTimeBetween(
                barber, date.atStartOfDay(), date.plusDays(1).atStartOfDay());
        if (existingAppointments == null) {
            existingAppointments = List.of();
        }

        LocalTime slot = OPENING_TIME;
        while (!slot.plusMinutes(totalDurationMinutes).isAfter(CLOSING_TIME)) {
            LocalDateTime start = LocalDateTime.of(date, slot);
            LocalDateTime end = start.plusMinutes(totalDurationMinutes);

            boolean isPastSlot = date.isBefore(LocalDate.now()) || (date.isEqual(LocalDate.now()) && start.isBefore(now));
            if (isPastSlot) {
                slot = slot.plusMinutes(SLOT_MINUTES);
                continue;
            }

            boolean conflict = existingAppointments.stream()
                    .filter(a -> a != null)
                    .filter(a -> a.getStatus() != AppointmentStatus.CANCELLED)
                    .filter(a -> a.getAppointmentDateTime() != null)
                    .filter(a -> getSafeDurationMinutes(a) != null)
                    .anyMatch(a -> overlaps(a, start, end));

            if (!conflict) {
                availableSlots.add(slot);
            }

            slot = slot.plusMinutes(SLOT_MINUTES);
        }

        return availableSlots;
    }

    public Appointment findById(Long id) {
        return appointmentRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found with id: " + id));
    }

    public Appointment update(Appointment appointment) {
        return save(appointment);
    }

    public Appointment updateStatus(Long id, AppointmentStatus status) {
        Appointment appointment = findById(id);
        appointment.setStatus(status);
        return appointmentRepository.save(appointment);
    }

    /**
     * Saves an appointment after verifying the chosen barber has no other
     * SCHEDULED appointment that overlaps the requested time slot.
     */
    public Appointment save(Appointment appointment) {
        if (appointment == null || appointment.getBarber() == null || appointment.getAppointmentDateTime() == null) {
            throw new IllegalArgumentException("Appointment, barber, and time are required.");
        }

        List<SalonService> selectedServices = appointment.getServices();
        if ((selectedServices == null || selectedServices.isEmpty()) && appointment.getService() == null) {
            throw new IllegalArgumentException("At least one service is required.");
        }

        if (selectedServices == null || selectedServices.isEmpty()) {
            selectedServices = List.of(appointment.getService());
            appointment.setServices(selectedServices);
        }

        int duration = appointment.getEffectiveDurationMinutes();
        if (duration <= 0) {
            throw new IllegalArgumentException("Selected services must include a valid duration.");
        }

        LocalDateTime start = appointment.getAppointmentDateTime();
        if (start.isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Appointment time must be current or in the future.");
        }

        LocalDateTime end = start.plusMinutes(duration);

        Barber barber = appointment.getBarber();
        List<Appointment> existing = appointmentRepository.findByBarberAndAppointmentDateTimeBetween(
                barber, start.minusHours(6), end.plusHours(6));

        boolean conflict = existing.stream()
                .filter(a -> a != null)
                .filter(a -> a.getId() == null || !a.getId().equals(appointment.getId()))
                .filter(a -> a.getStatus() != AppointmentStatus.CANCELLED)
                .anyMatch(a -> overlaps(a, start, end));

        if (conflict) {
            throw new IllegalStateException(
                    "Barber " + barber.getName() + " is already booked during that time slot.");
        }

        return appointmentRepository.save(appointment);
    }

    private boolean overlaps(Appointment existing, LocalDateTime start, LocalDateTime end) {
        Integer durationMinutes = getSafeDurationMinutes(existing);
        if (existing == null || existing.getAppointmentDateTime() == null || durationMinutes == null) {
            return false;
        }

        LocalDateTime existingStart = existing.getAppointmentDateTime();
        LocalDateTime existingEnd = existingStart.plusMinutes(durationMinutes);
        return start.isBefore(existingEnd) && existingStart.isBefore(end);
    }

    private Integer getSafeDurationMinutes(Appointment appointment) {
        if (appointment == null) {
            return null;
        }

        try {
            return appointment.getEffectiveDurationMinutes();
        } catch (LazyInitializationException ex) {
            return null;
        }
    }

    public void deleteById(Long id) {
        appointmentRepository.deleteById(id);
    }
}
