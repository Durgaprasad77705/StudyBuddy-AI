package com.aicoach.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, unique = true, length = 180)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(length = 20)
    @Builder.Default
    private String authProvider = "LOCAL";

    @Column(length = 120)
    private String resetToken;

    private LocalDateTime resetTokenExpiry;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Role role = Role.USER;

    @Column(length = 30)
    @Builder.Default
    private String subscriptionPlan = "NONE";

    private LocalDateTime subscriptionEndsAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean trialUsed = false;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum Role {
        USER,
        ADMIN
    }
}