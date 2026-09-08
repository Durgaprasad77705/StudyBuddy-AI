package com.aicoach.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Profile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(length = 150)
    private String education;

    @Column(length = 120)
    private String targetRole;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private ExperienceLevel experienceLevel = ExperienceLevel.ENTRY;

    @Lob
    private String bio;

    /** comma separated skill list, e.g. "Java,Spring Boot,SQL" */
    @Lob
    private String skills;

    public enum ExperienceLevel { ENTRY, MID, SENIOR }
}
