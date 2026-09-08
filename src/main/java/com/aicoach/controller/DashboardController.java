package com.aicoach.controller;

import com.aicoach.dto.DashboardDtos.DashboardResponse;
import com.aicoach.security.UserPrincipal;
import com.aicoach.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public DashboardResponse getDashboard(@AuthenticationPrincipal UserPrincipal principal) {
        return dashboardService.getDashboard(principal.getUserId());
    }
}
