package com.aicoach.repository;

import com.aicoach.entity.UserGamification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserGamificationRepository extends JpaRepository<UserGamification, Long> {
    Optional<UserGamification> findByUserId(Long userId);
}
