package com.example.salon.service;

import com.example.salon.controller.AppointmentController;
import com.example.salon.controller.BarberController;
import com.example.salon.controller.CustomerController;
import com.example.salon.controller.HomeController;
import com.example.salon.entity.Appointment;
import com.example.salon.entity.AppointmentStatus;
import com.example.salon.entity.Barber;
import com.example.salon.entity.Customer;
import com.example.salon.entity.SalonService;
import com.example.salon.repository.AppointmentRepository;
import com.example.salon.repository.BarberRepository;
import com.example.salon.repository.CustomerRepository;
import com.example.salon.repository.SalonServiceRepository;
import com.example.salon.service.BarberService;
import com.example.salon.service.CustomerService;
import com.example.salon.service.SalonServiceService;
import org.hibernate.LazyInitializationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private BarberRepository barberRepository;

    @Mock
    private SalonServiceRepository salonServiceRepository;

    @Mock
    private CustomerService customerService;

    @Mock
    private BarberService barberService;

    @Mock
    private SalonServiceService salonServiceService;

    @InjectMocks
    private AppointmentService appointmentService;

    @Test
    void appointmentController_editForm_shouldPopulateAppointmentData() {
        AppointmentController controller = new AppointmentController(
                appointmentService, customerService, barberService, salonServiceService);
        Appointment appointment = new Appointment();
        Customer customer = new Customer();
        customer.setId(3L);
        Barber barber = new Barber();
        barber.setId(4L);
        SalonService service = new SalonService();
        service.setId(5L);
        service.setDurationMinutes(45);

        appointment.setId(10L);
        appointment.setCustomer(customer);
        appointment.setBarber(barber);
        appointment.setService(service);
        appointment.setAppointmentDateTime(java.time.LocalDateTime.of(2026, 9, 19, 14, 30));

        when(appointmentRepository.findById(10L)).thenReturn(Optional.of(appointment));
        Model model = new ExtendedModelMap();

        String view = controller.editForm(10L, model);

        assertEquals("appointments/form", view);
        assertEquals(10L, ((Appointment) model.getAttribute("appointment")).getId());
        assertEquals(14, ((Appointment) model.getAttribute("appointment")).getAppointmentDateTime().getHour());
    }

    @Test
    void appointmentController_createForm_shouldPopulateCustomerNameFromExistingMobileNumber() {
        AppointmentController controller = new AppointmentController(
                appointmentService, customerService, barberService, salonServiceService);

        Customer customer = new Customer();
        customer.setId(1L);
        customer.setName("Alice");
        customer.setPhone("9876543210");

        when(customerService.findByPhone("9876543210")).thenReturn(customer);

        Model model = new ExtendedModelMap();
        String view = controller.createForm(model, null, null, "9876543210", null, null, LocalDate.of(2026, 9, 18));

        assertEquals("appointments/form", view);
        assertEquals("Alice", model.getAttribute("selectedCustomerName"));
        assertEquals("9876543210", model.getAttribute("selectedMobileNumber"));
    }

    @Test
    void salonServiceService_deleteById_shouldRejectWhenServiceIsInUse() {
        SalonService service = new SalonService();
        service.setId(99L);

        when(appointmentRepository.countByServiceId(99L)).thenReturn(1L);

        SalonServiceService serviceService = new SalonServiceService(salonServiceRepository, appointmentRepository);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> serviceService.deleteById(99L));
        assertTrue(ex.getMessage().contains("Cannot delete service"));
    }

    @Test
    void appointment_shouldSumMultipleSelectedServiceDurations() {
        Appointment appointment = new Appointment();
        SalonService haircut = new SalonService();
        haircut.setDurationMinutes(45);
        SalonService beardTrim = new SalonService();
        beardTrim.setDurationMinutes(30);

        appointment.getServices().add(haircut);
        appointment.getServices().add(beardTrim);
        appointment.setService(haircut);

        assertEquals(75, appointment.getEffectiveDurationMinutes());
    }

    @Test
    void appointmentController_createForm_shouldPopulateSelectedNames() {
        AppointmentController controller = new AppointmentController(
                appointmentService, customerService, barberService, salonServiceService);

        Customer customer = new Customer();
        customer.setId(1L);
        customer.setName("Alice");

        Barber barber = new Barber();
        barber.setId(2L);
        barber.setName("Sam");

        SalonService service = new SalonService();
        service.setId(3L);
        service.setName("Haircut");
        service.setDurationMinutes(60);

        when(customerService.findById(1L)).thenReturn(customer);
        when(barberService.findById(2L)).thenReturn(barber);
        when(salonServiceService.findById(3L)).thenReturn(service);
        when(appointmentRepository.findByBarberAndAppointmentDateTimeBetween(
                eq(barber), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());

        Model model = new ExtendedModelMap();
        String view = controller.createForm(model, 1L, 2L, 3L, LocalDate.of(2026, 9, 18));

        assertEquals("appointments/form", view);
        assertEquals("Alice", model.getAttribute("selectedCustomerName"));
        assertEquals("Sam", model.getAttribute("selectedBarberName"));
        assertEquals("Haircut", model.getAttribute("selectedServiceName"));
    }

    @Test
    void appointmentController_save_shouldCreateCustomerFromFreeTextNameAndPhone() {
        AppointmentController controller = new AppointmentController(
                appointmentService, customerService, barberService, salonServiceService);

        Barber barber = new Barber();
        barber.setId(2L);
        barber.setName("Sam");

        SalonService service = new SalonService();
        service.setId(3L);
        service.setName("Haircut");
        service.setDurationMinutes(60);

        Customer customer = new Customer();
        customer.setId(10L);
        customer.setName("Riya");
        customer.setPhone("9876543210");

        when(customerService.findOrCreateCustomer("Riya", "9876543210")).thenReturn(customer);
        when(barberService.findById(2L)).thenReturn(barber);
        when(salonServiceService.findById(3L)).thenReturn(service);
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Model model = new ExtendedModelMap();
        String view = controller.save(
                "Riya",
                "9876543210",
                2L,
                3L,
                LocalDateTime.of(2026, 9, 18, 11, 30),
                AppointmentStatus.SCHEDULED,
                model,
                new RedirectAttributesModelMap());

        assertEquals("redirect:/appointments", view);
    }

    @Test
    void appointmentController_updateStatus_shouldAllowPastAppointmentsWithoutTimeValidation() {
        AppointmentController controller = new AppointmentController(
                appointmentService, customerService, barberService, salonServiceService);

        Appointment appointment = new Appointment();
        appointment.setId(11L);
        appointment.setAppointmentDateTime(LocalDateTime.now().minusDays(2));
        appointment.setStatus(AppointmentStatus.SCHEDULED);

        when(appointmentRepository.findById(11L)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String view = controller.updateStatus(11L, AppointmentStatus.CANCELLED, new RedirectAttributesModelMap());

        assertEquals("redirect:/appointments", view);
        assertEquals(AppointmentStatus.CANCELLED, appointment.getStatus());
    }

    @Test
    void findFilteredAppointments_shouldRespectBarberAndDateFilters() {
        Barber barber = new Barber();
        barber.setId(12L);
        barber.setName("Nina");

        Appointment appointment = new Appointment();
        appointment.setId(77L);
        appointment.setBarber(barber);
        appointment.setAppointmentDateTime(LocalDateTime.of(2026, 9, 18, 11, 30));
        appointment.setStatus(AppointmentStatus.SCHEDULED);

        when(appointmentRepository.findFilteredAppointments(
                eq(12L),
                eq((String) null),
                eq((AppointmentStatus) null),
                any(LocalDateTime.class),
                any(LocalDateTime.class)))
                .thenReturn(List.of(appointment));

        List<Appointment> filtered = appointmentService.findFilteredAppointments(12L, null, LocalDate.of(2026, 9, 18));

        assertEquals(1, filtered.size());
        assertEquals(77L, filtered.get(0).getId());
    }

    @Test
    void findAvailableSlots_shouldExcludeBookedTimesForBarber() {
        Barber barber = new Barber();
        barber.setId(10L);

        SalonService service = new SalonService();
        service.setDurationMinutes(60);

        LocalDate date = LocalDate.of(2026, 9, 18);
        Appointment existing = new Appointment();
        existing.setId(99L);
        existing.setBarber(barber);
        existing.setService(service);
        existing.setAppointmentDateTime(LocalDateTime.of(date, LocalTime.of(10, 0)));
        existing.setStatus(AppointmentStatus.SCHEDULED);

        when(appointmentRepository.findByBarberAndAppointmentDateTimeBetween(
                eq(barber), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(existing));

        List<LocalTime> availableSlots = appointmentService.findAvailableSlots(barber, service, date);

        assertTrue(availableSlots.contains(LocalTime.of(9, 0)));
        assertFalse(availableSlots.contains(LocalTime.of(9, 30)));
        assertFalse(availableSlots.contains(LocalTime.of(10, 0)));
    }

    @Test
    void save_shouldRejectBookingWhenSameSlotIsAlreadyBookedForBarber() {
        Barber barber = new Barber();
        barber.setId(10L);
        barber.setName("Sam");

        SalonService service = new SalonService();
        service.setId(5L);
        service.setDurationMinutes(60);

        Appointment existing = new Appointment();
        existing.setId(99L);
        existing.setBarber(barber);
        existing.setService(service);
        existing.setAppointmentDateTime(LocalDateTime.of(2026, 9, 19, 14, 0));
        existing.setStatus(AppointmentStatus.SCHEDULED);

        when(appointmentRepository.findByBarberAndAppointmentDateTimeBetween(
                eq(barber), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(existing));

        Appointment newBooking = new Appointment();
        newBooking.setBarber(barber);
        newBooking.setService(service);
        newBooking.setAppointmentDateTime(LocalDateTime.of(2026, 9, 19, 14, 0));
        newBooking.setStatus(AppointmentStatus.SCHEDULED);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> appointmentService.save(newBooking));
        assertTrue(ex.getMessage().contains("already booked"));
    }

    @Test
    void save_shouldRejectBookingForPastTime() {
        Barber barber = new Barber();
        barber.setId(10L);
        barber.setName("Sam");

        SalonService service = new SalonService();
        service.setId(6L);
        service.setDurationMinutes(60);

        Appointment booking = new Appointment();
        booking.setBarber(barber);
        booking.setService(service);
        booking.setAppointmentDateTime(LocalDateTime.now().minusHours(1));
        booking.setStatus(AppointmentStatus.SCHEDULED);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> appointmentService.save(booking));
        assertTrue(ex.getMessage().contains("current or in the future"));
    }

    @Test
    void findAvailableSlots_shouldIgnoreLegacyAppointmentsWithoutServiceData() {
        Barber barber = new Barber();
        barber.setId(10L);

        SalonService service = new SalonService();
        service.setDurationMinutes(60);

        LocalDate date = LocalDate.of(2026, 9, 18);
        Appointment legacy = new Appointment();
        legacy.setId(100L);
        legacy.setBarber(barber);
        legacy.setAppointmentDateTime(LocalDateTime.of(date, LocalTime.of(10, 0)));
        legacy.setStatus(AppointmentStatus.SCHEDULED);

        when(appointmentRepository.findByBarberAndAppointmentDateTimeBetween(
                eq(barber), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(legacy));

        List<LocalTime> availableSlots = appointmentService.findAvailableSlots(barber, service, date);

        assertTrue(availableSlots.contains(LocalTime.of(9, 0)));
        assertTrue(availableSlots.contains(LocalTime.of(10, 0)));
    }

    @Test
    void findAvailableSlots_shouldIgnoreAppointmentsWithUninitializedLazyService() {
        Barber barber = new Barber();
        barber.setId(10L);

        SalonService service = new SalonService();
        service.setDurationMinutes(60);

        LocalDate date = LocalDate.of(2026, 9, 18);
        Appointment lazyAppointment = new Appointment();
        lazyAppointment.setId(101L);
        lazyAppointment.setBarber(barber);
        lazyAppointment.setAppointmentDateTime(LocalDateTime.of(date, LocalTime.of(10, 0)));
        lazyAppointment.setStatus(AppointmentStatus.SCHEDULED);
        lazyAppointment.setService(new SalonService() {
            @Override
            public Integer getDurationMinutes() {
                throw new LazyInitializationException("could not initialize proxy [com.example.salon.entity.SalonService#4] - no Session");
            }
        });

        when(appointmentRepository.findByBarberAndAppointmentDateTimeBetween(
                eq(barber), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(lazyAppointment));

        List<LocalTime> availableSlots = appointmentService.findAvailableSlots(barber, service, date);

        assertTrue(availableSlots.contains(LocalTime.of(9, 0)));
        assertTrue(availableSlots.contains(LocalTime.of(10, 0)));
    }

    @Test
    void findFilteredAppointments_shouldRespectStatusFilter() {
        Barber barber = new Barber();
        barber.setId(12L);
        barber.setName("Nina");

        Appointment appointment = new Appointment();
        appointment.setId(77L);
        appointment.setBarber(barber);
        appointment.setAppointmentDateTime(LocalDateTime.of(2026, 9, 18, 11, 30));
        appointment.setStatus(AppointmentStatus.SCHEDULED);

        when(appointmentRepository.findAllByOrderByAppointmentDateTimeDesc()).thenReturn(List.of(appointment));

        List<Appointment> filtered = appointmentService.findFilteredAppointments(12L, null, null, AppointmentStatus.SCHEDULED);

        assertEquals(1, filtered.size());
        assertEquals(AppointmentStatus.SCHEDULED, filtered.get(0).getStatus());
    }

    @Test
    void customerController_detail_shouldPopulateCustomerHistory() {
        CustomerController controller = new CustomerController(customerService, appointmentService);
        Customer customer = new Customer();
        customer.setId(15L);
        customer.setName("Jamie");

        SalonService service = new SalonService();
        service.setPrice(java.math.BigDecimal.valueOf(45.00));

        Appointment appointment = new Appointment();
        appointment.setId(88L);
        appointment.setCustomer(customer);
        appointment.setAppointmentDateTime(LocalDateTime.of(2026, 9, 18, 10, 0));
        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointment.setService(service);

        when(customerService.findById(15L)).thenReturn(customer);
        when(appointmentService.findAll()).thenReturn(List.of(appointment));

        Model model = new ExtendedModelMap();
        String view = controller.detail(15L, model);

        assertEquals("customers/detail", view);
        assertEquals("Jamie", ((Customer) model.getAttribute("customer")).getName());
        assertEquals(1, ((List<?>) model.getAttribute("appointments")).size());
    }

    @Test
    void barberController_detail_shouldPopulateBarberHistory() {
        BarberController controller = new BarberController(barberService, appointmentService);
        Barber barber = new Barber();
        barber.setId(21L);
        barber.setName("Alex");

        SalonService service = new SalonService();
        service.setName("Beard Trim");
        service.setPrice(java.math.BigDecimal.valueOf(30.00));

        Appointment appointment = new Appointment();
        appointment.setId(77L);
        appointment.setBarber(barber);
        appointment.setService(service);
        appointment.setAppointmentDateTime(LocalDateTime.of(2026, 9, 18, 11, 0));
        appointment.setStatus(AppointmentStatus.SCHEDULED);

        when(barberService.findById(21L)).thenReturn(barber);
        when(appointmentService.findAll()).thenReturn(List.of(appointment));

        Model model = new ExtendedModelMap();
        String view = controller.detail(21L, model);

        assertEquals("barbers/detail", view);
        assertEquals("Alex", ((Barber) model.getAttribute("barber")).getName());
        assertEquals(1, ((List<?>) model.getAttribute("appointments")).size());
    }

    @Test
    void homeController_dailyReport_shouldSummarizeRevenueByDate() {
        HomeController controller = new HomeController(
                customerRepository, barberRepository, salonServiceRepository, appointmentRepository);

        Customer customer = new Customer();
        customer.setId(10L);
        customer.setName("Jordan");

        SalonService service = new SalonService();
        service.setName("Haircut");
        service.setPrice(java.math.BigDecimal.valueOf(65.00));

        Appointment appointment = new Appointment();
        appointment.setCustomer(customer);
        appointment.setService(service);
        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointment.setAppointmentDateTime(LocalDateTime.of(2026, 9, 18, 10, 30));

        when(appointmentRepository.findAllByOrderByAppointmentDateTimeDesc()).thenReturn(List.of(appointment));

        Model model = new ExtendedModelMap();
        String view = controller.dailyReport(model);

        assertEquals("reports/daily", view);
        assertEquals(1, ((List<?>) model.getAttribute("dailyReports")).size());
        assertEquals(java.math.BigDecimal.valueOf(65.00), ((HomeController.DailyRevenueSummary) ((List<?>) model.getAttribute("dailyReports")).get(0)).getRevenue());
    }

    @Test
    void homeController_weeklyReport_shouldSummarizeRevenueByWeek() {
        HomeController controller = new HomeController(
                customerRepository, barberRepository, salonServiceRepository, appointmentRepository);

        Customer customer = new Customer();
        customer.setId(22L);
        customer.setName("Taylor");

        SalonService service = new SalonService();
        service.setName("Shave");
        service.setPrice(java.math.BigDecimal.valueOf(40.00));

        Appointment appointment = new Appointment();
        appointment.setCustomer(customer);
        appointment.setService(service);
        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointment.setAppointmentDateTime(LocalDateTime.of(2026, 9, 18, 9, 0));

        when(appointmentRepository.findAllByOrderByAppointmentDateTimeDesc()).thenReturn(List.of(appointment));

        Model model = new ExtendedModelMap();
        String view = controller.weeklyReport(model);

        assertEquals("reports/weekly", view);
        assertEquals(1, ((List<?>) model.getAttribute("weeklyReports")).size());
        assertEquals(java.math.BigDecimal.valueOf(40.00), ((HomeController.WeeklyRevenueSummary) ((List<?>) model.getAttribute("weeklyReports")).get(0)).getRevenue());
    }

    @Test
    void homeController_monthlyReport_shouldSummarizeRevenueByMonth() {
        HomeController controller = new HomeController(
                customerRepository, barberRepository, salonServiceRepository, appointmentRepository);

        Customer customer = new Customer();
        customer.setId(33L);
        customer.setName("Morgan");

        SalonService service = new SalonService();
        service.setName("Fade");
        service.setPrice(java.math.BigDecimal.valueOf(80.00));

        Appointment appointment = new Appointment();
        appointment.setCustomer(customer);
        appointment.setService(service);
        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointment.setAppointmentDateTime(LocalDateTime.of(2026, 9, 12, 13, 0));

        when(appointmentRepository.findAllByOrderByAppointmentDateTimeDesc()).thenReturn(List.of(appointment));

        Model model = new ExtendedModelMap();
        String view = controller.monthlyReport(model);

        assertEquals("reports/monthly", view);
        assertEquals(1, ((List<?>) model.getAttribute("monthlyReports")).size());
        assertEquals(java.math.BigDecimal.valueOf(80.00), ((HomeController.MonthlyRevenueSummary) ((List<?>) model.getAttribute("monthlyReports")).get(0)).getRevenue());
    }
}
