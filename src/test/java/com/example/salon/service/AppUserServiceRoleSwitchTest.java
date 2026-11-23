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
class AppUserServiceRoleSwitchTest {

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
    void shouldNotAllowSignInFlowToChangeAnExistingProfilesRole() {
        appUserRepository.save(new AppUser("Regular Staff", "7777888899", UserRole.REGULAR));

        assertThrows(IllegalArgumentException.class,
                () -> appUserService.createUser("Admin Staff", "7777888899", UserRole.ADMIN, new Barber()));
        assertEquals(UserRole.REGULAR, appUserRepository.findByPhone("7777888899").orElseThrow().getRole());
        assertEquals(1, appUserRepository.findAll().size());
    }
}
