package com.aicoach.service;

import com.aicoach.dto.DashboardDtos.DashboardResponse;
import com.aicoach.entity.InterviewSession;
import com.aicoach.entity.InterviewSession.SessionStatus;
import com.aicoach.entity.User;
import com.aicoach.entity.UserGamification;
import com.aicoach.exception.ResourceNotFoundException;
import com.aicoach.repository.InterviewSessionRepository;
import com.aicoach.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository userRepository;
    private final InterviewSessionRepository sessionRepository;
    private final GamificationService gamificationService;

    public DashboardResponse getDashboard(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<InterviewSession> all = sessionRepository.findByUserIdOrderByStartedAtDesc(userId);
        List<InterviewSession> completed = all.stream().filter(s -> s.getStatus() == SessionStatus.COMPLETED).toList();

        double avgScore = completed.stream()
                .mapToDouble(s -> s.getOverallScore() != null ? s.getOverallScore() : 0)
                .average().orElse(0);

        UserGamification g = gamificationService.getOrCreate(userId);

        List<String> recommendations = buildRecommendations(completed);

        return new DashboardResponse(
                user.getName(),
                all.size(),
                completed.size(),
                Math.round(avgScore * 10.0) / 10.0,
                g.getXp(),
                g.getLevel(),
                g.getCurrentStreak(),
                recommendations
        );
    }

    private List<String> buildRecommendations(List<InterviewSession> completed) {
        if (completed.isEmpty()) {
            return List.of("Complete your profile and take your first mock interview to get personalized recommendations.");
        }

        Map<String, List<Double>> byCategory = new LinkedHashMap<>();
        for (InterviewSession s : completed) {
            byCategory.computeIfAbsent("Communication", k -> new ArrayList<>())
                    .add(s.getCommunicationScore() != null ? s.getCommunicationScore() : 0);
            byCategory.computeIfAbsent("Confidence", k -> new ArrayList<>())
                    .add(s.getConfidenceScore() != null ? s.getConfidenceScore() : 0);
            byCategory.computeIfAbsent("Knowledge", k -> new ArrayList<>())
                    .add(s.getKnowledgeScore() != null ? s.getKnowledgeScore() : 0);
        }

        String weakest = null;
        double weakestAvg = Double.MAX_VALUE;
        for (Map.Entry<String, List<Double>> e : byCategory.entrySet()) {
            double avg = e.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0);
            if (avg < weakestAvg) {
                weakestAvg = avg;
                weakest = e.getKey();
            }
        }

        List<String> recs = new ArrayList<>();
        if (weakest != null) {
            recs.add("Focus on improving " + weakest.toLowerCase() + " — it's currently your weakest area (avg " +
                    Math.round(weakestAvg) + "/100).");
        }
        InterviewSession.InterviewType nextType = completed.get(0).getType() == InterviewSession.InterviewType.HR
                ? InterviewSession.InterviewType.TECHNICAL : InterviewSession.InterviewType.HR;
        recs.add("Try a " + nextType.name() + " interview next to round out your practice.");
        recs.add("Keep your streak alive — practice again today to grow your level and unlock badges.");
        return recs;
    }
}
