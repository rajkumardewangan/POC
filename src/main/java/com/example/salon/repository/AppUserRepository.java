package com.example.salon.repository;

import com.example.salon.entity.AppUser;
import com.example.salon.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByPhone(String phone);
    List<AppUser> findByRole(UserRole role);
    boolean existsByRole(UserRole role);
}
