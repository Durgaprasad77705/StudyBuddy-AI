package com.aicoach.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class GroupService {

    private final AiCoachService aiCoachService;

    private static final List<String> FALLBACK_TOPICS = List.of(
            "Will generative AI create more jobs than it replaces?",
            "Should companies adopt a four-day work week?",
            "Is remote work better than working from an office for software teams?",
            "Should cybersecurity be a mandatory skill for every software engineer?",
            "Can automation improve customer service without reducing human connection?",
            "Should companies prioritize skills over college degrees when hiring?",
            "How should organizations balance data privacy and AI innovation?",
            "Is open-source software more valuable than proprietary software for innovation?",
            "Should employees continuously reskill as technology changes?",
            "What makes a technology project successful beyond writing good code?"
    );

    public GroupService(AiCoachService aiCoachService) {
        this.aiCoachService = aiCoachService;
    }

    public String generateTopic(String role, String experience) {
        String safeRole = safe(role, "Any Job Role");
        String safeExperience = safe(experience, "Fresher");
        String prompt = """
                You are an expert group-discussion moderator for job interviews.
                Candidate role: %s
                Experience: %s

                Generate exactly ONE fresh group discussion topic that is useful for this role.
                It can be technical, business, workplace, AI, ethics or current-affairs related.
                Return exactly this structure:
                Topic: <one sentence>
                Background: <two short sentences>
                Discussion Instructions: <one short sentence>
                Do not provide a conclusion.
                """.formatted(safeRole, safeExperience);

        try {
            return aiCoachService.generateText(prompt, 450);
        } catch (Exception ignored) {
            return fallbackTopic(safeRole);
        }
    }

    public List<String> generateTopics(String role, String experience, int count) {
        int requested = Math.max(1, Math.min(count, 10));
        String safeRole = safe(role, "Any Job Role");
        String safeExperience = safe(experience, "Fresher");
        String prompt = """
                Generate exactly %d distinct job-interview group discussion topics for the role "%s" at "%s" level.
                Mix technical, workplace, business, AI, ethics and current-affairs themes.
                Return ONLY a numbered list, one topic per line, with no introduction and no conclusions.
                """.formatted(requested, safeRole, safeExperience);

        try {
            String raw = aiCoachService.generateText(prompt, 900);
            List<String> parsed = Arrays.stream(raw.split("\\R"))
                    .map(this::cleanTopicLine)
                    .filter(s -> !s.isBlank())
                    .distinct()
                    .limit(requested)
                    .toList();
            if (parsed.size() >= requested) return parsed;
        } catch (Exception ignored) {
            // Use the offline bank below so the button still works without an AI key.
        }

        List<String> fallback = new ArrayList<>();
        AtomicInteger index = new AtomicInteger(Math.floorMod(safeRole.toLowerCase(Locale.ROOT).hashCode(), FALLBACK_TOPICS.size()));
        while (fallback.size() < requested) {
            String topic = FALLBACK_TOPICS.get(index.getAndIncrement() % FALLBACK_TOPICS.size());
            if (!fallback.contains(topic)) fallback.add(topic);
        }
        return fallback;
    }

    public String generateParticipantResponse(String topic, String candidateStatement) {
        return generateParticipantResponse(topic, candidateStatement, "balanced professional participant");
    }

    public String generateParticipantResponse(String topic, String candidateStatement, String persona) {
        String safeTopic = safe(topic, "the current group discussion topic");
        String safeStatement = safe(candidateStatement, "I would like to share my opening point.");
        String prompt = """
                You are %s in a live job-interview group discussion.
                Topic: %s
                Candidate statement: %s

                Respond naturally in 2-3 short sentences. Support or respectfully challenge the candidate,
                add one useful point, and keep the discussion moving. Do not mention that you are an AI.
                """.formatted(safe(persona, "balanced professional participant"), safeTopic, safeStatement);

        try {
            return aiCoachService.generateText(prompt, 220);
        } catch (Exception ignored) {
            return "I agree that this is an important point. I would add that the practical impact depends on how organizations implement it. A good approach is to balance the benefits with measurable risks and involve the team before making a final decision.";
        }
    }

    public String evaluateDiscussion(String topic, String transcript) {
        String safeTopic = safe(topic, "the group discussion topic");
        String safeTranscript = safe(transcript, "No transcript was provided.");
        String prompt = """
                Evaluate this candidate's group discussion performance.
                Topic: %s
                Transcript:
                %s

                Return a clear interview-coach report with:
                Overall Score /100
                Communication /10
                Clarity /10
                Leadership /10
                Relevance /10
                Confidence /10
                Listening /10
                Logical Thinking /10
                Teamwork /10
                Strengths (3 bullets)
                Weaknesses (3 bullets)
                Suggestions (3 actionable bullets)
                """.formatted(safeTopic, safeTranscript);

        try {
            return aiCoachService.generateText(prompt, 900);
        } catch (Exception ignored) {
            return fallbackEvaluation(safeTranscript);
        }
    }

    private String fallbackTopic(String role) {
        String roleLower = role.toLowerCase(Locale.ROOT);
        if (roleLower.contains("java") || roleLower.contains("developer") || roleLower.contains("software")) {
            return "Topic: Should software teams prioritize rapid delivery or clean, maintainable code?\n"
                    + "Background: Fast delivery can help businesses respond to customers quickly, while technical debt can increase long-term cost.\n"
                    + "Discussion Instructions: Use one project example and explain how you would balance speed with quality.";
        }
        return "Topic: Should companies prioritize skills over college degrees when hiring?\n"
                + "Background: Skills-based hiring can widen the talent pool, while degrees can provide a consistent academic signal.\n"
                + "Discussion Instructions: Present both sides and finish with a practical hiring recommendation.";
    }

    private String fallbackEvaluation(String transcript) {
        int statements = (int) Arrays.stream(transcript.split("\\R"))
                .filter(line -> line.toLowerCase(Locale.ROOT).startsWith("you:"))
                .count();
        int words = transcript.trim().isEmpty() ? 0 : transcript.trim().split("\\s+").length;
        int score = Math.max(45, Math.min(90, 55 + Math.min(20, statements * 5) + Math.min(15, words / 40)));
        return "Overall Score /100: " + score + "\n"
                + "Communication /10: " + Math.min(9, 5 + statements) + "\n"
                + "Clarity /10: 7\nLeadership /10: 6\nRelevance /10: 7\nConfidence /10: 7\n"
                + "Listening /10: 6\nLogical Thinking /10: 7\nTeamwork /10: 7\n\n"
                + "Strengths\n- You participated and developed a clear point.\n- Your response stayed connected to the discussion.\n- You can improve further by using examples.\n\n"
                + "Weaknesses\n- Add stronger evidence or examples.\n- Explicitly connect your point to another participant.\n- End important responses with a concise conclusion.\n\n"
                + "Suggestions\n- Use Point → Example → Impact for each major contribution.\n- Acknowledge another view before disagreeing.\n- Keep each contribution focused and interview-ready.";
    }

    private String cleanTopicLine(String value) {
        return value.replaceFirst("^\\s*(?:[-*]|\\d+[.)])\\s*", "")
                .replaceFirst("^(?i)topic:\\s*", "")
                .trim();
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
