package com.example.salon.config;

import com.example.salon.entity.AppUser;
import com.example.salon.entity.Barber;
import com.example.salon.entity.SalonService;
import com.example.salon.entity.UserRole;
import com.example.salon.repository.AppUserRepository;
import com.example.salon.repository.BarberRepository;
import com.example.salon.repository.SalonServiceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DataSeeder implements CommandLineRunner {

    private final BarberRepository barberRepository;
    private final SalonServiceRepository salonServiceRepository;
    private final AppUserRepository appUserRepository;
    private final JdbcTemplate jdbcTemplate;

    @Value("${salon.bootstrap-admin.name:}")
    private String bootstrapAdminName;

    @Value("${salon.bootstrap-admin.phone:}")
    private String bootstrapAdminPhone;

    public DataSeeder(BarberRepository barberRepository,
                      SalonServiceRepository salonServiceRepository,
                      AppUserRepository appUserRepository,
                      JdbcTemplate jdbcTemplate) {
        this.barberRepository = barberRepository;
        this.salonServiceRepository = salonServiceRepository;
        this.appUserRepository = appUserRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        ensureBarberBaseSalaryColumn();
        ensureBarberWeeklyOffDayColumn();
        ensureAttendanceStatusColumn();
        ensureAdminUser();

        if (barberRepository.count() == 0) {
            Barber mike = new Barber("Mike Johnson", "Fades & Skin Fades", "555-0101");
            mike.setBaseSalary(new BigDecimal("30000"));
            barberRepository.save(mike);

            Barber carlos = new Barber("Carlos Rivera", "Beard Styling", "555-0102");
            carlos.setBaseSalary(new BigDecimal("28000"));
            barberRepository.save(carlos);

            Barber sam = new Barber("Sam Lee", "Classic Cuts", "555-0103");
            sam.setBaseSalary(new BigDecimal("32000"));
            barberRepository.save(sam);
        } else {
            barberRepository.findAll().forEach(barber -> {
                if (barber.getBaseSalary() == null) {
                    barber.setBaseSalary(BigDecimal.ZERO);
                }
                if (barber.getWeeklyOffDay() == null) {
                    barber.setWeeklyOffDay(java.time.DayOfWeek.SUNDAY);
                }
                barberRepository.save(barber);
            });
        }

        salonServiceRepository.findAll().forEach(service -> {
            if (service.getGender() == null) {
                service.setGender(com.example.salon.entity.ServiceGender.MEN);
                salonServiceRepository.save(service);
            }
        });

        if (salonServiceRepository.count() == 0) {
            salonServiceRepository.save(new SalonService("Classic Haircut", "Classic men's haircut", new BigDecimal("20.00"), 30, com.example.salon.entity.ServiceGender.MEN));
            salonServiceRepository.save(new SalonService("Beard Trim", "Beard shaping and trim", new BigDecimal("12.00"), 15, com.example.salon.entity.ServiceGender.MEN));
            salonServiceRepository.save(new SalonService("Hot Towel Shave", "Traditional straight razor shave", new BigDecimal("25.00"), 30, com.example.salon.entity.ServiceGender.MEN));
            salonServiceRepository.save(new SalonService("Haircut + Beard Combo", "Haircut and beard trim combo", new BigDecimal("30.00"), 45, com.example.salon.entity.ServiceGender.MEN));

            salonServiceRepository.save(new SalonService("Women's Cut", "Precision women’s haircut", new BigDecimal("35.00"), 45, com.example.salon.entity.ServiceGender.WOMEN));
            salonServiceRepository.save(new SalonService("Blowout Styling", "Salon blowout and finishing style", new BigDecimal("28.00"), 30, com.example.salon.entity.ServiceGender.WOMEN));
            salonServiceRepository.save(new SalonService("Hair Spa", "Deep nourishment and repair treatment", new BigDecimal("40.00"), 45, com.example.salon.entity.ServiceGender.WOMEN));
            salonServiceRepository.save(new SalonService("Salon Styling Package", "Cut, wash, and styling package", new BigDecimal("55.00"), 60, com.example.salon.entity.ServiceGender.WOMEN));
        }
    }

    private void ensureAdminUser() {
        if (appUserRepository.existsByRole(UserRole.ADMIN)) {
            return;
        }

        boolean hasName = bootstrapAdminName != null && !bootstrapAdminName.isBlank();
        boolean hasPhone = bootstrapAdminPhone != null && !bootstrapAdminPhone.isBlank();
        if (!hasName && !hasPhone) {
            return;
        }
        if (!hasName || !hasPhone) {
            throw new IllegalStateException("Configure both SALON_BOOTSTRAP_ADMIN_NAME and SALON_BOOTSTRAP_ADMIN_PHONE.");
        }

        String phone = bootstrapAdminPhone.replaceAll("[^0-9+]", "");
        if (!phone.matches("^[0-9+]{7,15}$")) {
            throw new IllegalStateException("SALON_BOOTSTRAP_ADMIN_PHONE must contain 7 to 15 digits.");
        }
        appUserRepository.save(new AppUser(bootstrapAdminName.trim(), phone, UserRole.ADMIN));
    }

    private void ensureBarberBaseSalaryColumn() {
        Long columnCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE UPPER(TABLE_NAME) = 'BARBERS' AND UPPER(COLUMN_NAME) = 'BASE_SALARY'",
                Long.class
        );

        if (columnCount != null && columnCount == 0) {
            jdbcTemplate.execute("ALTER TABLE barbers ADD COLUMN base_salary DECIMAL(19,2) DEFAULT 0 NOT NULL");
        }
    }

    private void ensureBarberWeeklyOffDayColumn() {
        Long columnCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE UPPER(TABLE_NAME) = 'BARBERS' AND UPPER(COLUMN_NAME) = 'WEEKLY_OFF_DAY'",
                Long.class
        );

        if (columnCount != null && columnCount == 0) {
            jdbcTemplate.execute("ALTER TABLE barbers ADD COLUMN weekly_off_day VARCHAR(20) DEFAULT 'SUNDAY' NOT NULL");
        }
    }

    private void ensureAttendanceStatusColumn() {
        String dataType = jdbcTemplate.queryForObject(
                "SELECT DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE UPPER(TABLE_NAME) = 'ATTENDANCES' AND UPPER(COLUMN_NAME) = 'STATUS'",
                String.class
        );

        if ("ENUM".equalsIgnoreCase(dataType)) {
            jdbcTemplate.execute("ALTER TABLE attendances ALTER COLUMN status SET DATA TYPE VARCHAR(32)");
        }
    }
}
