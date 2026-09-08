package com.aicoach.service.impl;

import com.aicoach.service.AiCoachService;

import java.util.List;

/**
 * Very simple offline heuristic used only as a safety-net fallback when the
 * configured AI provider is unreachable or no API key is set. It keeps the
 * product fully functional for local demos without external dependencies.
 */
public final class HeuristicEvaluator {

    private HeuristicEvaluator() {}

    public static AiCoachService.AnswerEvaluation evaluateAnswer(String answer) {
        int words = answer.trim().isEmpty() ? 0 : answer.trim().split("\\s+").length;
        double lengthScore = Math.min(100, words * 3.5);          // reward reasonably detailed answers
        double structureBonus = (answer.contains(".") || answer.contains(",")) ? 5 : 0;
        double score = Math.max(10, Math.min(100, lengthScore * 0.7 + 25 + structureBonus));

        String feedback;
        if (words < 15) {
            feedback = "Your answer is quite short. Try using the STAR method (Situation, Task, Action, Result) " +
                    "and add specific examples or numbers to strengthen your response.";
        } else if (words < 60) {
            feedback = "Good start. Add a bit more detail on the outcome/impact of your action to make the answer more compelling.";
        } else {
            feedback = "Solid, detailed answer. Consider trimming it slightly and leading with the key result for maximum impact.";
        }
        return new AiCoachService.AnswerEvaluation(round(score), feedback);
    }

    public static AiCoachService.SessionEvaluation evaluateSession(List<AiCoachService.AnswerEvaluation> answers) {
        double avg = answers.stream().mapToDouble(AiCoachService.AnswerEvaluation::score).average().orElse(50);
        double communication = round(Math.min(100, avg + 2));
        double confidence = round(Math.max(0, avg - 3));
        double knowledge = round(avg);
        double overall = round((communication + confidence + knowledge) / 3.0);

        return new AiCoachService.SessionEvaluation(
                communication, confidence, knowledge, overall,
                List.of("Clear structure in most answers", "Engaged well with the questions asked"),
                List.of("Could add more quantifiable results", "Some answers could be more concise"),
                List.of("Practice the STAR method for behavioral questions",
                        "Record yourself answering to improve delivery and pacing",
                        "Review core concepts for your target role weekly"),
                "Overall a " + (overall >= 75 ? "strong" : overall >= 50 ? "reasonable" : "developing") +
                        " performance. Keep practicing consistently to build confidence and depth."
        );
    }

    private static double round(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
