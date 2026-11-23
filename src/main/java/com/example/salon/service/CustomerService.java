package com.example.salon.service;

import com.example.salon.entity.Customer;
import com.example.salon.repository.CustomerRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public List<Customer> findAll() {
        return customerRepository.findAll();
    }

    public Customer findById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with id: " + id));
    }

    public Customer findByPhone(String phone) {
        String normalizedPhone = normalizePhone(phone);
        return customerRepository.findByPhone(normalizedPhone)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with mobile: " + normalizedPhone));
    }

    public Customer findOrCreateCustomer(String name, String phone) {
        String normalizedName = normalizeName(name);
        String normalizedPhone = normalizePhone(phone);

        if (normalizedName == null || normalizedName.isBlank()) {
            throw new IllegalArgumentException("Customer name is required.");
        }
        if (normalizedPhone == null || normalizedPhone.isBlank()) {
            throw new IllegalArgumentException("Mobile number is required.");
        }

        return customerRepository.findByPhone(normalizedPhone)
                .map(existing -> {
                    existing.setName(normalizedName);
                    existing.setPhone(normalizedPhone);
                    return customerRepository.save(existing);
                })
                .orElseGet(() -> {
                    Customer newCustomer = new Customer(normalizedName, normalizedPhone, null);
                    return customerRepository.save(newCustomer);
                });
    }

    public Customer save(Customer customer) {
        return customerRepository.save(customer);
    }

    public void deleteById(Long id) {
        customerRepository.deleteById(id);
    }

    private String normalizeName(String name) {
        return name == null ? null : name.trim();
    }

    private String normalizePhone(String phone) {
        return phone == null ? null : phone.trim();
    }
}
