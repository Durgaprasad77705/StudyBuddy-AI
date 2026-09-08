package com.aicoach.dto;

import com.aicoach.entity.Profile;

public class ProfileDtos {

    public record ProfileUpdateRequest(
            String education,
            String targetRole,
            Profile.ExperienceLevel experienceLevel,
            String bio,
            String skills
    ) {}

    public record ProfileResponse(
            Long userId,
            String name,
            String email,
            String education,
            String targetRole,
            Profile.ExperienceLevel experienceLevel,
            String bio,
            String skills,
            boolean complete
    ) {}
}
