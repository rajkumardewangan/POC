package com.example.salon.service;

import com.example.salon.entity.Barber;
import com.example.salon.entity.Appointment;
import com.example.salon.entity.AppointmentStatus;
import com.example.salon.entity.Customer;
import com.example.salon.entity.EmployeeWorkLog;
import com.example.salon.entity.SalonService;
import com.example.salon.repository.AppointmentRepository;
import com.example.salon.repository.AttendanceRepository;
import com.example.salon.repository.BarberRepository;
import com.example.salon.repository.CustomerRepository;
import com.example.salon.repository.EmployeeWorkLogRepository;
import com.example.salon.repository.SalonServiceRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WhatsAppBotServiceTest {

    private final AppointmentRepository appointmentRepository = mock(AppointmentRepository.class);
    private final BarberRepository barberRepository = mock(BarberRepository.class);
    private final SalonServiceRepository salonServiceRepository = mock(SalonServiceRepository.class);
    private final CustomerRepository customerRepository = mock(CustomerRepository.class);
    private final AttendanceRepository attendanceRepository = mock(AttendanceRepository.class);
    private final EmployeeWorkLogRepository workLogRepository = mock(EmployeeWorkLogRepository.class);
    private final AttendanceService attendanceService = mock(AttendanceService.class);

    private final WhatsAppBotService botService = new WhatsAppBotService(
            appointmentRepository,
            barberRepository,
            salonServiceRepository,
            customerRepository,
            attendanceRepository,
            workLogRepository,
            attendanceService,
            "+919999999999");

    @Test
    void employeeWorkingMessage_shouldPersistWorkLog() {
        Barber barber = new Barber("Sam", "Hair", "919876543210");
        barber.setId(7L);
        SalonService service = new SalonService("Haircut", "", java.math.BigDecimal.valueOf(300), 30);
        when(barberRepository.findAll()).thenReturn(List.of(barber));
        when(salonServiceRepository.findAll()).thenReturn(List.of(service));
        when(workLogRepository.findFirstByBarberIdAndEndedAtIsNullOrderByStartedAtDesc(7L))
                .thenReturn(Optional.empty());
        when(workLogRepository.save(any(EmployeeWorkLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<String> reply = botService.handleMessage("+91 98765 43210", "WORKING Haircut");

        assertTrue(reply.orElseThrow().contains("Logged your current work: Haircut"));
        verify(workLogRepository).save(any(EmployeeWorkLog.class));
    }

    @Test
    void employeeStartAppointment_shouldUpdateAssignedAppointmentAndCreateWorkLog() {
        Barber barber = new Barber("Sam", "Hair", "919876543210");
        barber.setId(7L);
        SalonService service = new SalonService("Haircut", "", java.math.BigDecimal.valueOf(300), 30);
        service.setId(11L);
        Customer customer = new Customer("Riya", "919812345678", null);
        Appointment appointment = new Appointment();
        appointment.setId(55L);
        appointment.setBarber(barber);
        appointment.setCustomer(customer);
        appointment.setServices(List.of(service));
        appointment.setService(service);
        appointment.setStatus(AppointmentStatus.SCHEDULED);

        when(barberRepository.findAll()).thenReturn(List.of(barber));
        when(appointmentRepository.findByIdWithDetails(55L)).thenReturn(Optional.of(appointment));
        when(workLogRepository.findFirstByBarberIdAndEndedAtIsNullOrderByStartedAtDesc(7L))
                .thenReturn(Optional.empty());
        when(workLogRepository.save(any(EmployeeWorkLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<String> reply = botService.handleMessage("919876543210", "START 55");

        assertTrue(reply.orElseThrow().contains("Started appointment 55"));
        assertTrue(appointment.getStatus() == AppointmentStatus.IN_PROGRESS);
        verify(appointmentRepository).save(appointment);
        verify(workLogRepository).save(any(EmployeeWorkLog.class));
    }

    @Test
    void ownerQuestion_shouldOnlyReplyForConfiguredOwner() {
        when(customerRepository.count()).thenReturn(23L);

        Optional<String> authorizedReply = botService.handleMessage("919999999999", "How many customers?");
        Optional<String> unauthorizedReply = botService.handleMessage("919111111111", "How many customers?");

        assertTrue(authorizedReply.orElseThrow().contains("23 customers"));
        assertTrue(unauthorizedReply.isEmpty());
    }
}