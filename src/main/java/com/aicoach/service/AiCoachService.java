package com.aicoach.service;

import com.aicoach.entity.InterviewSession;
import com.aicoach.entity.Profile;

import java.util.List;

/**
 * Abstraction over the AI provider used to (a) generate interview questions and
 * (b) evaluate answers. Swap the implementation (Anthropic, OpenAI, local model, etc.)
 * without touching InterviewService.
 */
public interface AiCoachService {

    List<String> generateQuestions(InterviewSession.InterviewType type,
                                    String targetRole,
                                    Profile.ExperienceLevel level,
                                    int count);

    AnswerEvaluation evaluateAnswer(String question, String answer, InterviewSession.InterviewType type);

    SessionEvaluation evaluateSession(InterviewSession.InterviewType type,
                                       String targetRole,
                                       List<QaPair> qaPairs);

    String generateFollowUpQuestion(InterviewSession.InterviewType type,
                                    String targetRole,
                                    String previousQuestion,
                                    String previousAnswer);

    /** Generate one free-form AI response for advanced modules such as Group Discussion. */
    String generateText(String prompt, int maxTokens);

    record QaPair(String question, String answer) {}

    record AnswerEvaluation(double score, String feedback) {}

    record SessionEvaluation(
            double communicationScore,
            double confidenceScore,
            double knowledgeScore,
            double overallScore,
            List<String> strengths,
            List<String> weaknesses,
            List<String> improvementPlan,
            String summary
    ) {}
}
