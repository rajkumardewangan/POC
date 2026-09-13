package com.example.salon.service;

import com.example.salon.entity.Barber;
import com.example.salon.repository.BarberRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BarberService {

    private final BarberRepository barberRepository;

    public BarberService(BarberRepository barberRepository) {
        this.barberRepository = barberRepository;
    }

    public List<Barber> findAll() {
        return barberRepository.findAll();
    }

    public Barber findById(Long id) {
        return barberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Barber not found with id: " + id));
    }

    public Barber save(Barber barber) {
        return barberRepository.save(barber);
    }

    public void deleteById(Long id) {
        barberRepository.deleteById(id);
    }
}
