package com.aicoach.service.impl;

import com.aicoach.dto.FutureFeatureDtos.FeatureAdviceResponse;
import com.aicoach.dto.FutureFeatureDtos.JobRecommendationResponse;
import com.aicoach.service.FutureFeaturesService;
import com.aicoach.dto.ResumeAnalysisResult;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class FutureFeaturesServiceImpl implements FutureFeaturesService {

    private final ResumeAnalysisEngine resumeAnalysisEngine;

    public FutureFeaturesServiceImpl(ResumeAnalysisEngine resumeAnalysisEngine) {
        this.resumeAnalysisEngine = resumeAnalysisEngine;
    }

    @Override
    public FeatureAdviceResponse voiceInterviewAdvice(String targetRole, String sampleAnswer) {
        return new FeatureAdviceResponse(
                "Voice Interview Practice",
                "Improve your verbal delivery and reduce filler words.",
                List.of(
                        "Speak clearly and at a steady pace to sound confident.",
                        "Record your answer and listen back to catch filler words.",
                        "Pause briefly between ideas so your responses feel natural.",
                        "Link your answer to the target role: " + targetRole
                ),
                List.of(
                        "Add examples that align with the job responsibilities.",
                        "Use specific metrics when possible to make your answer more concrete.",
                        sampleAnswer != null && !sampleAnswer.isBlank()
                                ? "Avoid repeating the same phrases in your sample answer."
                                : "Practice a strong opening sentence that summarizes your strengths."
                )
        );
    }

    @Override
    public FeatureAdviceResponse videoInterviewAdvice(String targetRole, String scenario) {
        return new FeatureAdviceResponse(
                "Video Interview Simulation",
                "Optimize your on-camera presence and preparation.",
                List.of(
                        "Choose a quiet, well-lit space with a neutral background.",
                        "Look at the camera to simulate eye contact with the interviewer.",
                        "Speak naturally and avoid rushing through your answers.",
                        "Dress professionally as if you were attending a live interview."
                ),
                List.of(
                        "Review your scenario and prepare 1-2 examples relevant to " + targetRole + ".",
                        scenario != null && !scenario.isBlank()
                                ? "Use the scenario to practice structure: context, action, result."
                                : "Prepare a short summary of your recent experience for introductions."
                )
        );
    }

    @Override
    public FeatureAdviceResponse companySpecificInterviewPrep(String companyName, String targetRole) {
        return new FeatureAdviceResponse(
                "Company-Specific Interview Prep",
                "Build answers around the company’s mission and role expectations.",
                List.of(
                        "Research the company’s values and recent news before the interview.",
                        "Identify 2-3 strengths that match the target role and company culture.",
                        "Prepare examples showing how you solved problems in a similar domain.",
                        "Use the company name frequently to make your answers feel tailored."
                ),
                List.of(
                        "Describe how your experience aligns with " + companyName + " and the role.",
                        "Mention any specific tools or processes used by the company if you know them.",
                        "Ask a thoughtful question about the company’s priorities at the end."
                )
        );
    }

    @Override
    public FeatureAdviceResponse codingInterviewPrep(String language, String problemType, String experienceLevel) {
        return new FeatureAdviceResponse(
                "Coding Interview Preparation",
                "Practice algorithmic reasoning, code clarity, and testable solutions.",
                List.of(
                        "Write clean, readable code and explain your thought process aloud.",
                        "Choose appropriate data structures for the problem type.",
                        "Use examples and edge case checks before finalizing your answer.",
                        "Test your solution with sample input and describe why it works."
                ),
                List.of(
                        "Practice common " + problemType + " questions in " + language + ".",
                        "Talk through your plan before you start coding.",
                        "Keep your code simple and refactor for readability.",
                        "Verify your solution against edge cases and boundary conditions."
                )
        );
    }

    @Override
    public FeatureAdviceResponse groupDiscussionPrep(String topic, String role) {
        return new FeatureAdviceResponse(
                "Group Discussion Practice",
                "Practice structure, collaboration, and concise persuasion.",
                List.of(
                        "Speak clearly and support your points with examples.",
                        "Listen actively and acknowledge others before adding your view.",
                        "Use structure: issue, point, example, impact, conclusion.",
                        "Stay calm and keep your responses focused on the main topic."
                ),
                List.of(
                        "If your role is " + role + ", emphasize leadership and decision-making.",
                        "Bring the discussion back to the main topic when it drifts.",
                        "Use data or facts when available to strengthen your argument."
                )
        );
    }

    @Override
    public FeatureAdviceResponse resumeAnalysis(String resumeText) {
        return new FeatureAdviceResponse(
                "Resume Analysis",
                "Optimize your resume to clearly highlight your experience and strengths.",
                List.of(
                        "Use concise bullet points that emphasize measurable outcomes.",
                        "Match your resume language to the job description.",
                        "Keep the most relevant experience near the top.",
                        "Avoid long paragraphs; use short, scannable lines instead."
                ),
                List.of(
                        "Add specific metrics, such as percentages and numbers, where possible.",
                        "Showcase tools and technologies used in each project.",
                        resumeText != null && resumeText.length() > 200
                                ? "Remove repetition and keep only the most relevant achievements."
                                : "Include one or two strong accomplishments for each role."
                )
        );
    }

    @Override
    public FeatureAdviceResponse resumeAnalysisFile(MultipartFile file) {
        return resumeAnalysis(parseResumeText(file));
    }

    private String parseResumeText(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Resume file is required.");
        }

        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        try {
            if (filename.endsWith(".pdf") || "application/pdf".equalsIgnoreCase(file.getContentType())) {
                try (PDDocument document = PDDocument.load(file.getInputStream())) {
                    PDFTextStripper stripper = new PDFTextStripper();
                    return stripper.getText(document).trim();
                }
            }

            if (filename.endsWith(".txt") || (file.getContentType() != null && file.getContentType().startsWith("text"))) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append('\n');
                    }
                    return sb.toString().trim();
                }
            }

            throw new IllegalArgumentException("Unsupported resume file type. Please upload a PDF or TXT file.");
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse resume file", e);
        }
    }

    @Override
    public ResumeAnalysisResult detailedResumeAnalysisFile(MultipartFile file) {
        return resumeAnalysisEngine.analyzeFile(file);
    }
    @Override
    public JobRecommendationResponse recommendJobs(String targetRole, List<String> skills) {
        List<String> normalized = skills.stream()
                .map(s -> s.toLowerCase(Locale.ROOT).trim())
                .collect(Collectors.toList());

        List<String> jobs = new ArrayList<>();
        if (targetRole != null && (targetRole.toLowerCase(Locale.ROOT).contains("developer") || targetRole.toLowerCase(Locale.ROOT).contains("engineer"))) {
            jobs.add("Software Engineer");
            jobs.add("Backend Developer");
            jobs.add("Full Stack Developer");
        } else if (targetRole != null && targetRole.toLowerCase(Locale.ROOT).contains("manager")) {
            jobs.add("Project Manager");
            jobs.add("Product Manager");
            jobs.add("Program Manager");
        } else {
            jobs.add("Business Analyst");
            jobs.add("Consultant");
            jobs.add("Customer Success Specialist");
        }

        if (normalized.contains("java") || normalized.contains("spring")) {
            jobs.add("Java Developer");
        }
        if (normalized.contains("data") || normalized.contains("sql")) {
            jobs.add("Data Analyst");
        }
        if (normalized.contains("cloud") || normalized.contains("aws") || normalized.contains("azure")) {
            jobs.add("Cloud Engineer");
        }

        List<String> reasons = List.of(
                "Matches your stated target role and current skills.",
                "Good fit for your experience with technical and soft skills.",
                "Recommended to help you progress toward higher-impact roles."
        );

        return new JobRecommendationResponse(jobs.stream().distinct().collect(Collectors.toList()), reasons);
    }


    @Override
    public com.aicoach.dto.FutureFeatureDtos.CareerPredictionResponse predictCareer(String targetRole, List<String> skills, String experience) {
        String role = targetRole == null ? "technology professional" : targetRole.trim();
        List<String> cleanSkills = skills == null ? List.of() : skills.stream().filter(x -> x != null && !x.isBlank()).map(String::trim).distinct().toList();
        String lower = (role + " " + String.join(" ", cleanSkills)).toLowerCase(Locale.ROOT);

        List<String> roles = new ArrayList<>();
        if (lower.contains("java") || lower.contains("spring")) roles.addAll(List.of("Java Developer", "Spring Boot Developer", "Full Stack Java Developer"));
        if (lower.contains("react") || lower.contains("angular") || lower.contains("node")) roles.add("Full Stack Developer");
        if (lower.contains("data") || lower.contains("python") || lower.contains("sql")) roles.addAll(List.of("Data Analyst", "Data Engineer"));
        if (lower.contains("ai") || lower.contains("machine learning") || lower.contains("ml")) roles.addAll(List.of("AI/ML Engineer", "Machine Learning Engineer"));
        if (roles.isEmpty()) roles.addAll(List.of(role, "Software Engineer", "Business Analyst"));
        roles = roles.stream().distinct().limit(5).collect(Collectors.toCollection(ArrayList::new));

        int fit = Math.min(96, 55 + cleanSkills.size() * 6 + (experience != null && !experience.isBlank() ? 8 : 0));
        int readiness = Math.min(94, 50 + cleanSkills.size() * 5);
        return new com.aicoach.dto.FutureFeatureDtos.CareerPredictionResponse(
                "AI predicts a strong path toward " + role + " based on the skills you entered.",
                List.of(
                        new com.aicoach.dto.FutureFeatureDtos.PredictionMetric("Career Fit", fit + "%"),
                        new com.aicoach.dto.FutureFeatureDtos.PredictionMetric("Interview Readiness", readiness + "%"),
                        new com.aicoach.dto.FutureFeatureDtos.PredictionMetric("Skills Used", String.valueOf(cleanSkills.size()))
                ),
                roles,
                List.of(
                        "Your target role and listed skills overlap with the recommended career paths.",
                        "Adding measurable project experience can improve recruiter confidence.",
                        "Interview practice should focus on the most important skills in the selected role."
                ),
                List.of(
                        "Build one strong project that demonstrates your top 2-3 skills.",
                        "Practice role-specific technical and behavioral interview questions.",
                        "Compare your skills with live job descriptions before applying.",
                        "Update your resume with measurable results and relevant keywords."
                )
        );
    }
}
