package com.aicoach.controller;

import com.aicoach.dto.ProfileDtos.*;
import com.aicoach.security.UserPrincipal;
import com.aicoach.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    public ProfileResponse getProfile(@AuthenticationPrincipal UserPrincipal principal) {
        return profileService.getProfile(principal.getUserId());
    }

    @PutMapping
    public ProfileResponse updateProfile(@AuthenticationPrincipal UserPrincipal principal,
                                          @Valid @RequestBody ProfileUpdateRequest request) {
        return profileService.updateProfile(principal.getUserId(), request);
    }
}
