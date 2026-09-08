package com.aicoach.service;

import org.springframework.stereotype.Service;

import com.aicoach.dto.InterviewRequest;

@Service
public class VideoService {

    private final OllamaService ollamaService;

    public VideoService(OllamaService ollamaService) {
        this.ollamaService = ollamaService;
    }

    public String generateQuestion(
            String role,
            String experience) {

        String prompt = """
                Act as an AI video interview interviewer.

                Role:
                %s

                Experience:
                %s

                Generate ONE interview question.

                Make it appropriate for a video interview.

                Focus on:
                - Technical knowledge
                - Communication
                - Project explanation
                - Behavioral skills

                Ask only one question.
                """.formatted(
                role,
                experience
        );

        return ollamaService.askAI(prompt);
    }

    public String evaluateTranscript(
            InterviewRequest request) {

        String prompt = """
                Evaluate this candidate's video interview response.

                Role:
                %s

                Question:
                %s

                Transcript:
                %s

                Analyze:

                1. Answer quality
                2. Technical correctness
                3. Communication
                4. Clarity
                5. Confidence indicators from speech
                6. Conciseness
                7. Filler words if visible in transcript
                8. Structure
                9. Score out of 100
                10. Suggestions

                Important:
                Do not claim to detect facial emotions from text alone.
                Only evaluate what can reasonably be inferred from
                the transcript.
                """.formatted(
                request.getRole(),
                request.getQuestion(),
                request.getTranscript()
        );

        return ollamaService.askAI(prompt);
    }
}