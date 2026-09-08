package com.aicoach.controller;

import com.aicoach.dto.DashboardDtos.GamificationResponse;
import com.aicoach.security.UserPrincipal;
import com.aicoach.service.GamificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/gamification")
@RequiredArgsConstructor
public class GamificationController {

    private final GamificationService gamificationService;

    @GetMapping("/status")
    public GamificationResponse status(@AuthenticationPrincipal UserPrincipal principal) {
        return gamificationService.getStatus(principal.getUserId());
    }
}
