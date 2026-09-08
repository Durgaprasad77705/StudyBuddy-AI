package com.aicoach.service;

import com.aicoach.dto.DashboardDtos.BadgeResponse;
import com.aicoach.dto.DashboardDtos.GamificationResponse;
import com.aicoach.entity.Badge;
import com.aicoach.entity.InterviewSession;
import com.aicoach.entity.User;
import com.aicoach.entity.UserBadge;
import com.aicoach.entity.UserGamification;
import com.aicoach.repository.BadgeRepository;
import com.aicoach.repository.InterviewSessionRepository;
import com.aicoach.repository.UserBadgeRepository;
import com.aicoach.repository.UserGamificationRepository;
import com.aicoach.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GamificationService {

    private static final int XP_PER_LEVEL = 500;

    private final UserGamificationRepository gamificationRepository;
    private final UserRepository userRepository;
    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final InterviewSessionRepository sessionRepository;

    /**
     * Called after an interview session is completed.
     * Awards XP, updates streaks and levels, and unlocks badges.
     */
    @Transactional
    public void onSessionCompleted(Long userId, double overallScore) {

        UserGamification g = getOrCreate(userId);

        // Base XP + score bonus
        int earnedXp = 50 + (int) Math.round(overallScore / 2.0);

        g.setXp(g.getXp() + earnedXp);

        // Calculate level
        g.setLevel(1 + g.getXp() / XP_PER_LEVEL);

        // Update practice streak
        LocalDate today = LocalDate.now();
        LocalDate last = g.getLastActivityDate();

        if (last == null || last.isBefore(today.minusDays(1))) {

            g.setCurrentStreak(1);

        } else if (last.equals(today.minusDays(1))) {

            g.setCurrentStreak(g.getCurrentStreak() + 1);

        }
        // If last == today, keep current streak unchanged

        g.setLongestStreak(
                Math.max(
                        g.getLongestStreak(),
                        g.getCurrentStreak()
                )
        );

        g.setLastActivityDate(today);

        gamificationRepository.save(g);

        // Check badges
        checkAndAwardBadges(
                userId,
                g,
                overallScore
        );
    }

    /**
     * Checks all badge conditions.
     */
    private void checkAndAwardBadges(
            Long userId,
            UserGamification g,
            double lastScore
    ) {

        long completed = sessionRepository.countByUserIdAndStatus(
                userId,
                InterviewSession.SessionStatus.COMPLETED
        );

        // Interview badges
        awardIfEligible(
                userId,
                "FIRST_INTERVIEW",
                "First Steps",
                "Completed your first mock interview",
                "🎉",
                completed >= 1
        );

        awardIfEligible(
                userId,
                "FIVE_INTERVIEWS",
                "Getting Serious",
                "Completed 5 mock interviews",
                "🔥",
                completed >= 5
        );

        awardIfEligible(
                userId,
                "TWENTY_INTERVIEWS",
                "Interview Veteran",
                "Completed 20 mock interviews",
                "🏆",
                completed >= 20
        );

        // Streak badges
        awardIfEligible(
                userId,
                "STREAK_3",
                "On a Roll",
                "3-day practice streak",
                "⚡",
                g.getCurrentStreak() >= 3
        );

        awardIfEligible(
                userId,
                "STREAK_7",
                "Consistency Champion",
                "7-day practice streak",
                "💪",
                g.getCurrentStreak() >= 7
        );

        // Score badge
        awardIfEligible(
                userId,
                "HIGH_SCORE",
                "Top Performer",
                "Scored 90+ in a mock interview",
                "⭐",
                lastScore >= 90
        );

        // Level badge
        awardIfEligible(
                userId,
                "LEVEL_5",
                "Rising Star",
                "Reached level 5",
                "🌟",
                g.getLevel() >= 5
        );
    }

    /**
     * Creates a badge if the user qualifies and hasn't already earned it.
     */
    private void awardIfEligible(
            Long userId,
            String code,
            String name,
            String description,
            String icon,
            boolean eligible
    ) {

        if (!eligible) {
            return;
        }

        // Don't award duplicate badge
        if (userBadgeRepository.existsByUserIdAndBadgeCode(
                userId,
                code
        )) {
            return;
        }

        // Find existing badge or create it
        Badge badge = badgeRepository
                .findByCode(code)
                .orElseGet(() ->
                        badgeRepository.save(
                                Badge.builder()
                                    .code(code)
                                        .name(name)
                                        .description(description)
                                        .icon(icon)
                                        .build()
                        )
                );

        User user = userRepository.getReferenceById(userId);

        UserBadge userBadge = UserBadge.builder()
                .user(user)
                .badge(badge)
                .build();

        userBadgeRepository.save(userBadge);
    }

    /**
     * Returns gamification status for dashboard.
     */
    public GamificationResponse getStatus(Long userId) {

        UserGamification g = getOrCreate(userId);

        List<UserBadge> earned =
                userBadgeRepository.findByUserIdOrderByEarnedAtDesc(
                        userId
                );

        List<BadgeResponse> badgeResponses = earned.stream()
                .map(ub ->
                        new BadgeResponse(
                                ub.getBadge().getCode(),
                                ub.getBadge().getName(),
                                ub.getBadge().getDescription(),
                                ub.getBadge().getIcon(),
                                true
                        )
                )
                .toList();

        int xpIntoLevel = g.getXp() % XP_PER_LEVEL;

        int xpForNext =
                XP_PER_LEVEL - xpIntoLevel;

        return new GamificationResponse(
                g.getXp(),
                g.getLevel(),
                xpForNext,
                g.getCurrentStreak(),
                g.getLongestStreak(),
                badgeResponses
        );
    }

    /**
     * Gets existing gamification record
     * or creates one for the user.
     */
    @Transactional
    public UserGamification getOrCreate(Long userId) {

        return gamificationRepository
                .findByUserId(userId)
                .orElseGet(() -> {

                    User user =
                            userRepository.getReferenceById(userId);

                    return gamificationRepository.save(
                            UserGamification.builder()
                                    .user(user)
                                    .xp(0)
                                    .level(1)
                                    .currentStreak(0)
                                    .longestStreak(0)
                                    .build()
                    );
                });
    }
}