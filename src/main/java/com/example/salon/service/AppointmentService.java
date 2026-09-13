package com.example.salon.service;

import com.example.salon.entity.Appointment;
import com.example.salon.entity.AppointmentStatus;
import com.example.salon.entity.Barber;
import com.example.salon.repository.AppointmentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AppointmentService {

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

    public Appointment findById(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found with id: " + id));
    }

    /**
     * Saves an appointment after verifying the chosen barber has no other
     * SCHEDULED appointment that overlaps the requested time slot.
     */
    public Appointment save(Appointment appointment) {
        LocalDateTime start = appointment.getAppointmentDateTime();
        int duration = appointment.getService().getDurationMinutes();
        LocalDateTime end = start.plusMinutes(duration);

        Barber barber = appointment.getBarber();
        List<Appointment> existing = appointmentRepository.findByBarberAndAppointmentDateTimeBetween(
                barber, start.minusHours(6), end.plusHours(6));

        boolean conflict = existing.stream()
                .filter(a -> !a.getId().equals(appointment.getId()))
                .filter(a -> a.getStatus() != AppointmentStatus.CANCELLED)
                .anyMatch(a -> overlaps(a, start, end));

        if (conflict) {
            throw new IllegalStateException(
                    "Barber " + barber.getName() + " is already booked during that time slot.");
        }

        return appointmentRepository.save(appointment);
    }

    private boolean overlaps(Appointment existing, LocalDateTime start, LocalDateTime end) {
        LocalDateTime existingStart = existing.getAppointmentDateTime();
        LocalDateTime existingEnd = existingStart.plusMinutes(existing.getService().getDurationMinutes());
        return start.isBefore(existingEnd) && existingStart.isBefore(end);
    }

    public void deleteById(Long id) {
        appointmentRepository.deleteById(id);
    }
}
