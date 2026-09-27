package com.aicoach.controller;

import com.aicoach.entity.User;
import com.aicoach.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/recovery")
@RequiredArgsConstructor
public class DatabaseRecoveryController {

    private final UserRepository userRepository;

    @Value("${app.recovery.secret:}")
    private String recoverySecret;

    @GetMapping("/users")
    public ResponseEntity<?> getUsers(
            @RequestHeader(value = "X-Recovery-Secret", required = false) String secret) {

        if (recoverySecret.isBlank() || secret == null || !recoverySecret.equals(secret)) {
            return ResponseEntity.status(403).body("Access denied");
        }

        List<UserBackup> users = userRepository.findAll()
                .stream()
                .map(user -> new UserBackup(
                        user.getId(),
                        user.getName(),
                        user.getEmail(),
                        user.getAuthProvider(),
                        user.getRole().name(),
                        user.getSubscriptionPlan(),
                        user.getSubscriptionEndsAt(),
                        user.isTrialUsed(),
                        user.getCreatedAt()
                ))
                .toList();

        return ResponseEntity.ok(users);
    }

    public record UserBackup(
            Long id,
            String name,
            String email,
            String authProvider,
            String role,
            String subscriptionPlan,
            LocalDateTime subscriptionEndsAt,
            boolean trialUsed,
            LocalDateTime createdAt
    ) {}
}