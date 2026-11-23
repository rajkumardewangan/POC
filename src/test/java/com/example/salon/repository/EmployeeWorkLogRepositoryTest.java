package com.example.salon.repository;

import com.example.salon.entity.Appointment;
import com.example.salon.entity.AppointmentStatus;
import com.example.salon.entity.Barber;
import com.example.salon.entity.Customer;
import com.example.salon.entity.EmployeeWorkLog;
import com.example.salon.entity.SalonService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
class EmployeeWorkLogRepositoryTest {

    @Autowired
    private EmployeeWorkLogRepository workLogRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private BarberRepository barberRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private SalonServiceRepository salonServiceRepository;

    @Test
    void activeWorkLogQuery_shouldFetchDashboardActivityDetails() {
        Barber barber = new Barber("Sam", "Hair", "919876543210");
        barber = barberRepository.save(barber);
        Customer customer = customerRepository.save(new Customer("Riya", "919812345678", null));
        SalonService service = salonServiceRepository.save(
                new SalonService("Haircut", "", BigDecimal.valueOf(300), 30));

        Appointment appointment = new Appointment();
        appointment.setCustomer(customer);
        appointment.setBarber(barber);
        appointment.setServices(List.of(service));
        appointment.setService(service);
        appointment.setAppointmentDateTime(LocalDateTime.now());
        appointment.setStatus(AppointmentStatus.IN_PROGRESS);
        appointment = appointmentRepository.save(appointment);

        EmployeeWorkLog workLog = new EmployeeWorkLog();
        workLog.setBarber(barber);
        workLog.setAppointment(appointment);
        workLog.setStartedAt(LocalDateTime.now());
        workLogRepository.saveAndFlush(workLog);

        List<EmployeeWorkLog> activeLogs = workLogRepository.findByEndedAtIsNullOrderByStartedAtDesc();

        assertEquals(1, activeLogs.size());
        assertEquals("Sam", activeLogs.get(0).getBarber().getName());
        assertEquals("Haircut", activeLogs.get(0).getActivitySummary());
    }
}