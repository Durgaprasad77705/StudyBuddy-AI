package com.aicoach.service;

import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class CodingService {

    private final OllamaService ollamaService;

    public CodingService(OllamaService ollamaService) {
        this.ollamaService = ollamaService;
    }

    public String generateProblem(String language, String problemType, String experienceLevel) {
        String prompt = """
                You are an expert technical interviewer.
                Create ONE realistic coding interview problem.
                Language: %s
                Problem Type: %s
                Experience Level: %s
                Return:
                1. Problem title
                2. Problem statement
                3. Input format
                4. Output format
                5. Example input
                6. Example output
                7. Constraints
                8. Hints
                Do NOT provide the complete solution or final code.
                """.formatted(language, problemType, experienceLevel);
        try {
            return ollamaService.askAI(prompt);
        } catch (Exception e) {
            return fallbackProblem(language, problemType, experienceLevel);
        }
    }

    public String evaluateCode(String problem, String candidateCode, String language) {
        String prompt = """
                You are evaluating a coding interview candidate.
                Problem: %s
                Candidate Language: %s
                Candidate Code: %s
                Analyze:
                1. Correctness
                2. Logical approach
                3. Time complexity
                4. Space complexity
                5. Edge cases
                6. Code quality
                7. Bugs
                8. Interview communication
                9. Score out of 100
                10. Improvement suggestions
                Be strict like a real technical interviewer.
                """.formatted(problem, language, candidateCode);
        try {
            return ollamaService.askAI(prompt);
        } catch (Exception e) {
            return fallbackEvaluation(candidateCode, language);
        }
    }

    private String fallbackProblem(String language, String type, String level) {
        return "CODING INTERVIEW PROBLEM\n\n"
                + "Title: Longest Unique Substring\n\n"
                + "Experience: " + level + "\n"
                + "Language: " + language + "\n"
                + "Problem: Given a string, return the length of the longest substring without repeating characters.\n\n"
                + "Input: One string s.\n"
                + "Output: An integer representing the maximum length.\n"
                + "Example Input: abcabcbb\n"
                + "Example Output: 3\n"
                + "Constraints: 0 <= s.length <= 100000.\n"
                + "Hint: Consider a sliding window and a map of last seen positions.\n\n"
                + "AI status: Ollama is currently unavailable. This local interview problem was generated so you can continue practicing. Start Ollama with 'ollama serve' for AI-generated problems.";
    }

    private String fallbackEvaluation(String code, String language) {
        int score = 25;
        String lower = code == null ? "" : code.toLowerCase(Locale.ROOT);
        if (!lower.isBlank()) score += 20;
        if (lower.contains("map") || lower.contains("hash") || lower.contains("set") || lower.contains("array")) score += 15;
        if (lower.contains("for") || lower.contains("while")) score += 10;
        if (code != null && code.length() > 80) score += 10;
        score = Math.min(80, score);
        return "CODING INTERVIEW EVALUATION\n\n"
                + "Language: " + language + "\n"
                + "Score: " + score + "/100\n\n"
                + "Correctness: Needs verification with the full test suite.\n"
                + "Logical approach: Code structure was inspected using a local fallback evaluator.\n"
                + "Time complexity: Explain the complexity of each loop and data-structure operation.\n"
                + "Space complexity: State the additional memory used.\n"
                + "Edge cases: Test empty input, one element, duplicates and maximum-size input.\n"
                + "Code quality: Use descriptive names and keep methods focused.\n"
                + "Improvement: Start Ollama with 'ollama serve' and keep llama3.2:3b installed for full AI evaluation.";
    }
}
