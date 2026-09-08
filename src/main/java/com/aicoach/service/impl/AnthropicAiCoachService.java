package com.aicoach.service.impl;

import com.aicoach.config.AiProperties;
import com.aicoach.entity.InterviewSession.InterviewType;
import com.aicoach.entity.Profile.ExperienceLevel;
import com.aicoach.service.AiCoachService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Talks to the Anthropic Messages API to generate interview questions and evaluate answers.
 * If no API key is configured, or any call fails, it transparently falls back to the
 * offline StaticQuestionBank / HeuristicEvaluator so the product keeps working.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnthropicAiCoachService implements AiCoachService {

    private final AiProperties aiProperties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private boolean aiEnabled() {
        return "anthropic".equalsIgnoreCase(aiProperties.getProvider())
                && aiProperties.getApiKey() != null
                && !aiProperties.getApiKey().isBlank();
    }

    // ---------------------------------------------------------------- questions

    @Override
    public List<String> generateQuestions(InterviewType type, String targetRole, ExperienceLevel level, int count) {
        if (!aiEnabled()) {
            return StaticQuestionBank.pick(type, count);
        }
        try {
            String prompt = """
                    You are an expert interview coach. Generate %d realistic interview questions for a %s interview
                    for the role of "%s", targeting a candidate with %s experience level.
                    Return ONLY a raw JSON array of question strings, with no markdown fences and no commentary.
                    Example format: ["question 1", "question 2"]
                    """.formatted(count, type, safe(targetRole, "the target role"), level);

            String raw = callClaude(prompt, 1024);
            JsonNode node = objectMapper.readTree(stripFences(raw));
            List<String> questions = new ArrayList<>();
            if (node.isArray()) {
                for (JsonNode q : node) questions.add(q.asText());
            }
            if (questions.isEmpty()) throw new IllegalStateException("Empty question list from AI");
            return questions.size() > count ? questions.subList(0, count) : questions;
        } catch (Exception e) {
            log.warn("AI question generation failed, falling back to static bank: {}", e.getMessage());
            return StaticQuestionBank.pick(type, count);
        }
    }

    // ---------------------------------------------------------------- single answer

    @Override
    public AnswerEvaluation evaluateAnswer(String question, String answer, InterviewType type) {
        if (!aiEnabled()) {
            return HeuristicEvaluator.evaluateAnswer(answer);
        }
        try {
            String prompt = """
                    You are an expert interview coach evaluating ONE answer from a %s interview.
                    Question: %s
                    Candidate answer: %s

                    Score the answer from 0 to 100 based on relevance, clarity, structure and depth.
                    Provide brief, constructive, specific feedback (2-3 sentences).
                    Return ONLY raw JSON, no markdown fences: {"score": <number 0-100>, "feedback": "<string>"}
                    """.formatted(type, question, answer);

            String raw = callClaude(prompt, 512);
            JsonNode node = objectMapper.readTree(stripFences(raw));
            double score = node.path("score").asDouble(50);
            String feedback = node.path("feedback").asText("Thanks for your answer.");
            return new AnswerEvaluation(clamp(score), feedback);
        } catch (Exception e) {
            log.warn("AI answer evaluation failed, using heuristic fallback: {}", e.getMessage());
            return HeuristicEvaluator.evaluateAnswer(answer);
        }
    }

    // ---------------------------------------------------------------- whole session

    @Override
    public SessionEvaluation evaluateSession(InterviewType type, String targetRole, List<QaPair> qaPairs) {
        if (!aiEnabled()) {
            List<AnswerEvaluation> individual = qaPairs.stream()
                    .map(qa -> HeuristicEvaluator.evaluateAnswer(qa.answer()))
                    .collect(Collectors.toList());
            return HeuristicEvaluator.evaluateSession(individual);
        }
        try {
            StringBuilder qaBlock = new StringBuilder();
            int i = 1;
            for (QaPair qa : qaPairs) {
                qaBlock.append(i++).append(". Q: ").append(qa.question())
                        .append("\n   A: ").append(qa.answer()).append("\n");
            }

            String prompt = """
                    You are an expert interview coach producing a final holistic report for a mock %s interview
                    for the role of "%s". Here are the question/answer pairs:

                    %s

                    Evaluate holistically across the whole session and return ONLY raw JSON (no markdown fences)
                    with exactly these fields:
                    {
                      "communicationScore": <0-100 number>,
                      "confidenceScore": <0-100 number>,
                      "knowledgeScore": <0-100 number>,
                      "overallScore": <0-100 number>,
                      "strengths": ["short bullet", "short bullet"],
                      "weaknesses": ["short bullet", "short bullet"],
                      "improvementPlan": ["short actionable bullet", "short actionable bullet"],
                      "summary": "2-3 sentence overall summary"
                    }
                    """.formatted(type, safe(targetRole, "the target role"), qaBlock);

            String raw = callClaude(prompt, 1536);
            JsonNode node = objectMapper.readTree(stripFences(raw));

            return new SessionEvaluation(
                    clamp(node.path("communicationScore").asDouble(60)),
                    clamp(node.path("confidenceScore").asDouble(60)),
                    clamp(node.path("knowledgeScore").asDouble(60)),
                    clamp(node.path("overallScore").asDouble(60)),
                    toList(node.path("strengths")),
                    toList(node.path("weaknesses")),
                    toList(node.path("improvementPlan")),
                    node.path("summary").asText("Keep practicing to build consistency and confidence.")
            );
        } catch (Exception e) {
            log.warn("AI session evaluation failed, using heuristic fallback: {}", e.getMessage());
            List<AnswerEvaluation> individual = qaPairs.stream()
                    .map(qa -> HeuristicEvaluator.evaluateAnswer(qa.answer()))
                    .collect(Collectors.toList());
            return HeuristicEvaluator.evaluateSession(individual);
        }
    }

    @Override
    public String generateFollowUpQuestion(InterviewType type, String targetRole, String previousQuestion, String previousAnswer) {
        if (!aiEnabled()) {
            return "Can you explain your approach in more detail?";
        }
        try {
            String prompt = """
                    You are an expert interviewer conducting a %s interview for the role of "%s".
                    The last question was: %s
                    The candidate answered: %s

                    Based on that answer, generate one appropriate follow-up interview question.
                    Decide whether to probe deeper, clarify, switch topic, or make the next question slightly harder.
                    Return ONLY the raw question text with no numbering or quotes.
                    """.formatted(type, safe(targetRole, "the target role"), previousQuestion, previousAnswer);

            String raw = callClaude(prompt, 256);
            return stripFences(raw).trim();
        } catch (Exception e) {
            log.warn("AI follow-up question generation failed, using fallback: {}", e.getMessage());
            return "Can you describe a specific example of how you solved a similar problem?";
        }
    }

    @Override
    public String generateText(String prompt, int maxTokens) {
        if (!aiEnabled()) {
            throw new IllegalStateException("AI provider is not configured");
        }
        try {
            return stripFences(callClaude(prompt, maxTokens)).trim();
        } catch (Exception e) {
            log.warn("Free-form AI generation failed: {}", e.getMessage());
            throw new IllegalStateException("AI provider is temporarily unavailable", e);
        }
    }

    // ---------------------------------------------------------------- helpers

    private String callClaude(String userPrompt, int maxTokens) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", aiProperties.getApiKey());
        headers.set("anthropic-version", "2023-06-01");

        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", aiProperties.getModel());
        body.put("max_tokens", maxTokens);
        ArrayNode messages = body.putArray("messages");
        ObjectNode message = messages.addObject();
        message.put("role", "user");
        message.put("content", userPrompt);

        HttpEntity<String> entity = new HttpEntity<>(body.toString(), headers);
        JsonNode response = restTemplate.postForObject(aiProperties.getBaseUrl(), entity, JsonNode.class);

        if (response == null || !response.has("content") || response.path("content").isEmpty()) {
            throw new IllegalStateException("Empty response from AI provider");
        }
        return response.path("content").get(0).path("text").asText("");
    }

    private String stripFences(String text) {
        String t = text.trim();
        if (t.startsWith("```")) {
            t = t.replaceFirst("^```[a-zA-Z]*", "").trim();
            if (t.endsWith("```")) t = t.substring(0, t.length() - 3).trim();
        }
        return t;
    }

    private List<String> toList(JsonNode arrayNode) {
        List<String> list = new ArrayList<>();
        if (arrayNode.isArray()) arrayNode.forEach(n -> list.add(n.asText()));
        return list;
    }

    private double clamp(double v) {
        return Math.max(0, Math.min(100, Math.round(v * 10.0) / 10.0));
    }

    private String safe(String v, String fallback) {
        return (v == null || v.isBlank()) ? fallback : v;
    }
}
