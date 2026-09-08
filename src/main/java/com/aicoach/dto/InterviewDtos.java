package com.aicoach.dto;

import com.aicoach.entity.InterviewSession;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public class InterviewDtos {

    public record StartSessionRequest(
            @NotNull InterviewSession.InterviewType type,
            String targetRole,
            boolean timed
    ) {}

    public record QuestionResponse(
            Long id,
            String text,
            String category,
            int orderIndex,
            boolean answered
    ) {}

    public record SessionResponse(
            Long id,
            InterviewSession.InterviewType type,
            String targetRole,
            boolean timed,
            InterviewSession.SessionStatus status,
            LocalDateTime startedAt,
            List<QuestionResponse> questions
    ) {}

    public record SubmitAnswerRequest(
            @NotNull Long questionId,
            @NotBlank String answerText,
            Integer timeTakenSeconds
    ) {}

    public record QuestionFeedback(
            Long questionId,
            String questionText,
            String answerText,
            double score,
            String feedback
    ) {}

    public record LiveAnswerRequest(
            @NotNull Long questionId,
            @NotBlank String transcript,
            Integer timeTakenSeconds,
            Double latencySeconds
    ) {}

    public record LiveAnswerResponse(
            Long questionId,
            String questionText,
            double aiScore,
            String aiFeedback,
            Long nextQuestionId,
            String nextQuestionText,
            int nextQuestionNumber,
            int totalQuestions,
            boolean completed,
            int wordCount,
            int fillerWordCount,
            double speakingPaceWpm,
            String analysis
    ) {}

    public record LiveStatusResponse(
            Long sessionId,
            String state,
            Long currentQuestionId,
            String currentQuestionText,
            int questionNumber,
            int totalQuestions,
            boolean completed,
            String statusMessage
    ) {}

    public record ReportResponse(
            Long sessionId,
            InterviewSession.InterviewType type,
            String targetRole,
            LocalDateTime startedAt,
            LocalDateTime completedAt,
            double overallScore,
            double communicationScore,
            double confidenceScore,
            double knowledgeScore,
            List<String> strengths,
            List<String> weaknesses,
            List<String> improvementPlan,
            String summary,
            List<QuestionFeedback> questionFeedback
    ) {}

    public record HistoryItem(
            Long id,
            InterviewSession.InterviewType type,
            String targetRole,
            InterviewSession.SessionStatus status,
            LocalDateTime startedAt,
            LocalDateTime completedAt,
            Double overallScore
    ) {}

    public record ProgressPoint(
            Long sessionId,
            LocalDateTime completedAt,
            double overallScore,
            double communicationScore,
            double confidenceScore,
            double knowledgeScore
    ) {}

    public record ProgressResponse(
            List<ProgressPoint> points,
            double averageScore,
            double trendDelta,
            int currentStreak,
            int longestStreak
    ) {}
}
