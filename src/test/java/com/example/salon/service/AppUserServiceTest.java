package com.example.salon.service;

import com.example.salon.entity.AppUser;
import com.example.salon.entity.Barber;
import com.example.salon.entity.UserRole;
import com.example.salon.repository.AppUserRepository;
import com.example.salon.repository.BarberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class AppUserServiceTest {

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private BarberRepository barberRepository;

    private AppUserService appUserService;

    @BeforeEach
    void setUp() {
        appUserService = new AppUserService(appUserRepository, barberRepository);
    }

    @Test
    void shouldCreateProfileWithAdminAssignedRole() {
        Barber staff = new Barber();
        staff.setSpecialty("Hair stylist");
        staff.setBaseSalary(new java.math.BigDecimal("30000"));
        staff.setHousingAllowance(new java.math.BigDecimal("2000"));
        AppUser saved = appUserService.createUser("Regular User", "987-654-3210", UserRole.REGULAR, staff);

        assertNotNull(saved);
        assertEquals(UserRole.REGULAR, saved.getRole());
        assertEquals("Regular User", saved.getName());
        assertEquals("9876543210", saved.getPhone());
        assertNotNull(saved.getBarber());
        assertEquals(new java.math.BigDecimal("2000"), saved.getBarber().getHousingAllowance());
        assertTrue(appUserRepository.findByPhone("9876543210").isPresent());
    }

    @Test
    void shouldRejectDuplicatePhoneWhenCreatingProfile() {
        appUserRepository.save(new AppUser("Existing", "1112223333", UserRole.REGULAR));

        assertThrows(IllegalArgumentException.class,
                () -> appUserService.createUser("Another", "1112223333", UserRole.ADMIN, new Barber()));
        assertEquals(UserRole.REGULAR, appUserRepository.findByPhone("1112223333").orElseThrow().getRole());
    }

    @Test
    void shouldUpdateProfileAndProtectLastAdmin() {
        AppUser admin = appUserRepository.save(new AppUser("Admin", "9999999999", UserRole.ADMIN));

        assertThrows(IllegalArgumentException.class,
                () -> appUserService.updateUser(admin.getId(), "Regular", "9999999999", UserRole.REGULAR, new Barber()));

            AppUser updated = appUserService.updateUser(admin.getId(), "Updated Admin", "9999999998", UserRole.ADMIN, new Barber());
        assertEquals("Updated Admin", updated.getName());
        assertEquals("9999999998", updated.getPhone());
    }

    @Test
    void shouldDeleteRegularProfile() {
        AppUser regular = appUserRepository.save(new AppUser("Regular", "8888888888", UserRole.REGULAR));

        appUserService.deleteUser(regular.getId());

        assertTrue(appUserRepository.findById(regular.getId()).isEmpty());
    }
}
