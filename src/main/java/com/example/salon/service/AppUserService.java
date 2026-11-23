package com.example.salon.service;

import com.example.salon.entity.AppUser;
import com.example.salon.entity.Barber;
import com.example.salon.entity.UserRole;
import com.example.salon.repository.AppUserRepository;
import com.example.salon.repository.BarberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class AppUserService {

    private final AppUserRepository appUserRepository;
    private final BarberRepository barberRepository;

    public AppUserService(AppUserRepository appUserRepository, BarberRepository barberRepository) {
        this.appUserRepository = appUserRepository;
        this.barberRepository = barberRepository;
    }

    public List<AppUser> findAll() {
        return appUserRepository.findAll();
    }

    public AppUser findById(Long id) {
        return appUserRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    public Optional<AppUser> findByPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return Optional.empty();
        }
        return appUserRepository.findByPhone(normalizePhone(phone));
    }

    @Transactional
    public AppUser createUser(String name, String phone, UserRole role, Barber staffProfile) {
        String normalizedPhone = normalizePhone(phone);
        validateProfile(name, normalizedPhone, role);
        if (appUserRepository.findByPhone(normalizedPhone).isPresent()) {
            throw new IllegalArgumentException("A profile with this mobile number already exists.");
        }
        AppUser user = new AppUser(name.trim(), normalizedPhone, role);
        if (role == UserRole.REGULAR) {
            user.setBarber(saveStaffProfile(null, name, normalizedPhone, staffProfile));
        }
        return appUserRepository.save(user);
    }

    @Transactional
    public AppUser updateUser(Long id, String name, String phone, UserRole role, Barber staffProfile) {
        AppUser user = findById(id);
        String normalizedPhone = normalizePhone(phone);
        validateProfile(name, normalizedPhone, role);
        appUserRepository.findByPhone(normalizedPhone)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("A profile with this mobile number already exists.");
                });
        if (user.getRole() == UserRole.ADMIN && role != UserRole.ADMIN
                && appUserRepository.findByRole(UserRole.ADMIN).size() <= 1) {
            throw new IllegalArgumentException("The last admin profile cannot be changed to a regular user.");
        }
        user.setName(name.trim());
        user.setPhone(normalizedPhone);
        user.setRole(role);
        if (role == UserRole.REGULAR) {
            user.setBarber(saveStaffProfile(user.getBarber(), name, normalizedPhone, staffProfile));
        }
        return appUserRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        AppUser user = findById(id);
        if (user.getRole() == UserRole.ADMIN
                && appUserRepository.findByRole(UserRole.ADMIN).size() <= 1) {
            throw new IllegalArgumentException("The last admin profile cannot be deleted.");
        }
        appUserRepository.delete(user);
    }

    private void validateProfile(String name, String phone, UserRole role) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name is required.");
        }
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("Mobile number is required.");
        }
        if (!phone.matches("^[0-9+]{7,15}$")) {
            throw new IllegalArgumentException("Enter a valid mobile number.");
        }
        if (role == null) {
            throw new IllegalArgumentException("Select a role.");
        }
    }

    private Barber saveStaffProfile(Barber existingProfile, String name, String phone, Barber submittedProfile) {
        if (submittedProfile == null || submittedProfile.getSpecialty() == null || submittedProfile.getSpecialty().isBlank()) {
            throw new IllegalArgumentException("Staff specialty is required for a regular user profile.");
        }
        Barber barber = existingProfile == null ? new Barber() : existingProfile;
        barber.setName(name.trim());
        barber.setPhone(phone);
        barber.setSpecialty(submittedProfile.getSpecialty().trim());
        barber.setEmail(submittedProfile.getEmail());
        barber.setAddress(submittedProfile.getAddress());
        barber.setEmergencyContactName(submittedProfile.getEmergencyContactName());
        barber.setEmergencyContactPhone(submittedProfile.getEmergencyContactPhone());
        barber.setJoiningDate(submittedProfile.getJoiningDate());
        barber.setEmploymentType(submittedProfile.getEmploymentType());
        barber.setBaseSalary(submittedProfile.getBaseSalary());
        barber.setHousingAllowance(submittedProfile.getHousingAllowance());
        barber.setTransportAllowance(submittedProfile.getTransportAllowance());
        barber.setOtherAllowance(submittedProfile.getOtherAllowance());
        barber.setFixedSalaryDeduction(submittedProfile.getFixedSalaryDeduction());
        barber.setWeeklyOffDay(submittedProfile.getWeeklyOffDay());
        return barberRepository.save(barber);
    }

    public String normalizePhone(String phone) {
        if (phone == null) {
            return "";
        }
        return phone.replaceAll("[^0-9+]", "").trim();
    }
}
