package com.aicoach.service;

import com.aicoach.dto.ProfileDtos.*;
import com.aicoach.entity.Profile;
import com.aicoach.entity.User;
import com.aicoach.exception.ResourceNotFoundException;
import com.aicoach.repository.ProfileRepository;
import com.aicoach.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;

    public ProfileResponse getProfile(Long userId) {
        Profile profile = getOrCreate(userId);
        return toResponse(profile);
    }

    @Transactional
    public ProfileResponse updateProfile(Long userId, ProfileUpdateRequest request) {
        Profile profile = getOrCreate(userId);
        profile.setEducation(request.education());
        profile.setTargetRole(request.targetRole());
        if (request.experienceLevel() != null) profile.setExperienceLevel(request.experienceLevel());
        profile.setBio(request.bio());
        profile.setSkills(request.skills());
        profileRepository.save(profile);
        return toResponse(profile);
    }

    private Profile getOrCreate(Long userId) {
        return profileRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            return profileRepository.save(Profile.builder().user(user).build());
        });
    }

    private ProfileResponse toResponse(Profile p) {
        boolean complete = p.getEducation() != null && !p.getEducation().isBlank()
                && p.getTargetRole() != null && !p.getTargetRole().isBlank()
                && p.getExperienceLevel() != null;
        return new ProfileResponse(
                p.getUser().getId(),
                p.getUser().getName(),
                p.getUser().getEmail(),
                p.getEducation(),
                p.getTargetRole(),
                p.getExperienceLevel(),
                p.getBio(),
                p.getSkills(),
                complete
        );
    }
}
