package com.aicoach.dto;

import java.util.List;

public class DashboardDtos {

    public record DashboardResponse(
            String name,
            long totalInterviews,
            long completedInterviews,
            double averageScore,
            int xp,
            int level,
            int currentStreak,
            List<String> recommendedPractice
    ) {}

    public record BadgeResponse(
            String code,
            String name,
            String description,
            String icon,
            boolean earned
    ) {}

    public record GamificationResponse(
            int xp,
            int level,
            int xpForNextLevel,
            int currentStreak,
            int longestStreak,
            List<BadgeResponse> badges
    ) {}
}
