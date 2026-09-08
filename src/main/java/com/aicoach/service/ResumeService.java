package com.aicoach.service;

import org.springframework.stereotype.Service;

@Service
public class ResumeService {

    private final OllamaService ollamaService;

    public ResumeService(OllamaService ollamaService) {
        this.ollamaService = ollamaService;
    }

    public String analyzeResume(String resumeText) {

        if (resumeText == null ||
                resumeText.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Resume content cannot be empty"
            );
        }

        String prompt = """
                You are an expert technical recruiter,
                resume analyzer and AI interview coach.

                Analyze the following candidate resume.

                RESUME:
                -------------------------
                %s
                -------------------------

                Provide a detailed but structured analysis.

                Use exactly these sections:

                1. CANDIDATE SUMMARY
                2. EDUCATION
                3. TECHNICAL SKILLS
                4. PROGRAMMING LANGUAGES
                5. FRAMEWORKS AND TECHNOLOGIES
                6. PROJECTS
                7. EXPERIENCE
                8. CERTIFICATIONS
                9. KEY STRENGTHS
                10. SKILL GAPS
                11. RESUME IMPROVEMENTS
                12. SUITABLE JOB ROLES
                13. INTERVIEW READINESS SCORE
                14. TOP 10 INTERVIEW QUESTIONS

                For the interview questions, generate questions
                specifically from the candidate's actual skills,
                projects and technologies.

                Do not invent qualifications that are not present
                in the resume.

                Keep the analysis practical for a technical
                job interview.
                """.formatted(resumeText);

        return ollamaService.ask(prompt);
    }
}