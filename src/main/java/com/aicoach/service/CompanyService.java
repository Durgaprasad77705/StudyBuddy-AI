package com.aicoach.service;

import org.springframework.stereotype.Service;

import com.aicoach.dto.InterviewRequest;

@Service
public class CompanyService {

    private final OllamaService ollamaService;

    public CompanyService(OllamaService ollamaService) {
        this.ollamaService = ollamaService;
    }

    public String generateQuestion(
            String company,
            String role,
            String experience) {

        String prompt = """
                You are an expert technical interviewer.

                Company:
                %s

                Role:
                %s

                Experience:
                %s

                Create ONE realistic interview question
                suitable for this role.

                Cover topics such as:

                Java
                OOP
                DSA
                SQL
                Spring Boot
                REST APIs
                Projects
                Behavioral questions

                Do not provide the answer.

                Important:
                Do not invent confidential company information.
                Ask a generally appropriate company-targeted question.
                """.formatted(
                company,
                role,
                experience
        );

        try { return ollamaService.askAI(prompt); } catch(Exception e) { return fallbackQuestion(company, role, experience); }
    }

    public String evaluate(
            InterviewRequest request) {

        String prompt = """
                Evaluate a candidate's interview answer.

                Company:
                %s

                Role:
                %s

                Question:
                %s

                Candidate Answer:
                %s

                Give:

                Score /10
                Correctness
                Technical depth
                Communication
                Missing points
                Strengths
                Weaknesses
                Better approach
                Next question
                """.formatted(
                request.getCompany(),
                request.getRole(),
                request.getQuestion(),
                request.getAnswer()
        );

        try { return ollamaService.askAI(prompt); } catch(Exception e) { return fallbackEvaluation(request); }
    }

    private String fallbackQuestion(String company,String role,String experience){
        return "Company: "+company+" | Role: "+role+" | Experience: "+experience+"\n\nInterview Question: Explain a Java/Spring Boot project you built, the hardest technical problem you faced, and how you measured the result.\n\nTip: Answer using Situation, Task, Action and Result (STAR).";
    }
    private String fallbackEvaluation(InterviewRequest r){
        String a=r.getAnswer()==null?"":r.getAnswer().trim(); int score=Math.min(10,Math.max(2,a.length()/80));
        return "Score /10: "+score+"\nTechnical depth: "+(a.length()>180?"Good detail":"Add more technical detail")+"\nCommunication: "+(a.length()>80?"Clear enough":"Give a structured answer")+"\nMissing points: Include your exact technology, decision, trade-off and measurable result.\nStrength: You attempted the question.\nNext question: What would you improve in that solution if traffic increased 10x?";
    }
}
