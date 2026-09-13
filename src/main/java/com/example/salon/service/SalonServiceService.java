package com.example.salon.service;

import com.example.salon.entity.SalonService;
import com.example.salon.repository.SalonServiceRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SalonServiceService {

    private final SalonServiceRepository salonServiceRepository;

    public SalonServiceService(SalonServiceRepository salonServiceRepository) {
        this.salonServiceRepository = salonServiceRepository;
    }

    public List<SalonService> findAll() {
        return salonServiceRepository.findAll();
    }

    public SalonService findById(Long id) {
        return salonServiceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Service not found with id: " + id));
    }

    public SalonService save(SalonService service) {
        return salonServiceRepository.save(service);
    }

    public void deleteById(Long id) {
        salonServiceRepository.deleteById(id);
    }
}
