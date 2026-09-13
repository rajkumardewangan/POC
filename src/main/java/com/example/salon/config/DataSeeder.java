package com.example.salon.config;

import com.example.salon.entity.Barber;
import com.example.salon.entity.SalonService;
import com.example.salon.repository.BarberRepository;
import com.example.salon.repository.SalonServiceRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DataSeeder implements CommandLineRunner {

    private final BarberRepository barberRepository;
    private final SalonServiceRepository salonServiceRepository;

    public DataSeeder(BarberRepository barberRepository, SalonServiceRepository salonServiceRepository) {
        this.barberRepository = barberRepository;
        this.salonServiceRepository = salonServiceRepository;
    }

    @Override
    public void run(String... args) {
        if (barberRepository.count() == 0) {
            barberRepository.save(new Barber("Mike Johnson", "Fades & Skin Fades", "555-0101"));
            barberRepository.save(new Barber("Carlos Rivera", "Beard Styling", "555-0102"));
            barberRepository.save(new Barber("Sam Lee", "Classic Cuts", "555-0103"));
        }

        if (salonServiceRepository.count() == 0) {
            salonServiceRepository.save(new SalonService("Haircut", "Classic men's haircut", new BigDecimal("20.00"), 30));
            salonServiceRepository.save(new SalonService("Beard Trim", "Beard shaping and trim", new BigDecimal("12.00"), 15));
            salonServiceRepository.save(new SalonService("Hot Towel Shave", "Traditional straight razor shave", new BigDecimal("25.00"), 30));
            salonServiceRepository.save(new SalonService("Haircut + Beard Combo", "Haircut and beard trim combo", new BigDecimal("30.00"), 45));
        }
    }
}
