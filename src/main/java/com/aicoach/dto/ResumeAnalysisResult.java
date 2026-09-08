package com.aicoach.dto;

import java.util.List;
import java.util.Map;

public record ResumeAnalysisResult(
        String candidateSummary,
        String education,
        List<String> skills,
        List<String> projects,
        List<String> experience,
        String certifications,
        List<String> strengths,
        List<String> gaps,
        List<String> improvements,
        Map<String, Integer> sectionScores,
        int overallScore,
        String readiness,
        boolean aiPowered,
        String analysisNote
) {}
