package com.aicoach.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class FutureFeatureDtos {

    public record VoiceInterviewRequest(
            @NotBlank String targetRole,
            String sampleAnswer
    ) {}

    public record VideoInterviewRequest(
            @NotBlank String targetRole,
            String scenario
    ) {}

    public record CompanyInterviewRequest(
            @NotBlank String companyName,
            @NotBlank String targetRole
    ) {}

    public record CodingInterviewRequest(
            @NotBlank String language,
            @NotBlank String problemType,
            String experienceLevel
    ) {}

    public record GroupDiscussionRequest(
            @NotBlank String topic,
            String role
    ) {}

    public record ResumeAnalysisRequest(
            @NotBlank String resumeText
    ) {}

    public record JobRecommendationRequest(
            @NotBlank String targetRole,
            @NotEmpty List<String> skills
    ) {}

    public record CareerPredictionRequest(
            @NotBlank String targetRole,
            @NotEmpty List<String> skills,
            String experience
    ) {}

    public record PredictionMetric(String label, String value) {}

    public record CareerPredictionResponse(
            String headline,
            List<PredictionMetric> metrics,
            List<String> recommendedRoles,
            List<String> reasons,
            List<String> nextSteps
    ) {}

    public record FeatureAdviceResponse(
            String feature,
            String headline,
            List<String> tips,
            List<String> suggestions
    ) {}

    public record JobRecommendationResponse(
            List<String> recommendedJobs,
            List<String> reasons
    ) {}
}
