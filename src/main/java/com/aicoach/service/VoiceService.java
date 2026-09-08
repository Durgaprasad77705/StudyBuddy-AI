package com.aicoach.service;

import org.springframework.stereotype.Service;

import com.aicoach.dto.InterviewRequest;

@Service
public class VoiceService {

    private final OllamaService ollamaService;

    public VoiceService(OllamaService ollamaService) {
        this.ollamaService = ollamaService;
    }

    public String generateQuestion(String role, String experienceLevel) {
        String prompt = """
                You are an expert voice interview interviewer.

                Create ONE realistic voice/verbal interview question.

                Role: %s
                Experience Level: %s

                Return:
                1. The interview question
                2. Expected answer points
                3. What to listen for in the answer
                4. Follow-up questions

                Keep it natural and conversational.
                """.formatted(role, experienceLevel);

        return ollamaService.askAI(prompt);
    }

    public String evaluateAnswer(InterviewRequest request) {
        String prompt = """
                You are evaluating a voice interview candidate's answer.

                Role: %s
                Question: %s
                Candidate's Answer: %s

                Analyze:
                1. Did they understand the question?
                2. Relevance of answer
                3. Communication skills
                4. Clarity and articulation
                5. Professional tone
                6. Confidence level
                7. Score out of 100
                8. Feedback for improvement

                Be constructive but honest.
                """.formatted(
                request.getRole(),
                request.getQuestion(),
                request.getAnswer()
        );

        return ollamaService.askAI(prompt);
    }
}
