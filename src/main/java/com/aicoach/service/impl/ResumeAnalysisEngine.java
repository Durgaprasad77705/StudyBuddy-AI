package com.aicoach.service.impl;

import com.aicoach.dto.ResumeAnalysisResult;
import com.aicoach.service.OllamaService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ResumeAnalysisEngine {
    private final OllamaService ollamaService;
    private final ObjectMapper objectMapper;

    public ResumeAnalysisEngine(OllamaService ollamaService, ObjectMapper objectMapper) {
        this.ollamaService = ollamaService;
        this.objectMapper = objectMapper;
    }

    public ResumeAnalysisResult analyzeFile(MultipartFile file) {
        String text = parse(file);
        return analyzeText(text);
    }

    public ResumeAnalysisResult analyzeText(String text) {
        if (text == null || text.isBlank()) throw new IllegalArgumentException("Could not extract readable text from the resume.");
        String clean = text.replace("\u0000", " ").trim();
        try {
            String prompt = """
                    You are a senior technical recruiter. Analyze the resume below.
                    Return ONLY valid JSON, no markdown and no code fences.
                    Required JSON keys:
                    candidateSummary (string), education (string), skills (array of strings),
                    projects (array of strings), experience (array of strings), certifications (string),
                    strengths (array of strings), gaps (array of strings), improvements (array of strings),
                    sectionScores (object with exactly: SKILLS, EDUCATION, PROJECTS, EXPERIENCE, SUMMARY, CERTIFICATIONS),
                    overallScore (integer 0-100), readiness (string).
                    Scores must be based only on evidence in the resume. Never invent qualifications.
                    Keep each array concise and practical.

                    RESUME TEXT:
                    %s
                    """.formatted(clean.substring(0, Math.min(clean.length(), 18000)));
            String raw = ollamaService.ask(prompt);
            JsonNode node = parseJson(raw);
            if (node != null && node.isObject()) return fromJson(node);
        } catch (Exception ignored) {
            // Local fallback keeps the application usable when Ollama is stopped.
        }
        return heuristic(clean);
    }

    private JsonNode parseJson(String raw) throws Exception {
        if (raw == null) return null;
        String s = raw.trim().replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "").trim();
        return objectMapper.readTree(s);
    }

    private ResumeAnalysisResult fromJson(JsonNode n) {
        Map<String,Integer> scores = new LinkedHashMap<>();
        String[] keys = {"SKILLS","EDUCATION","PROJECTS","EXPERIENCE","SUMMARY","CERTIFICATIONS"};
        for (String k : keys) scores.put(k, Math.max(0, Math.min(100, n.path("sectionScores").path(k).asInt(0))));
        int overall = Math.max(0, Math.min(100, n.path("overallScore").asInt(scores.values().stream().mapToInt(Integer::intValue).sum()/6)));
        return new ResumeAnalysisResult(
                n.path("candidateSummary").asText(""),
                n.path("education").asText("Not clearly detected"),
                strings(n.path("skills")), strings(n.path("projects")), strings(n.path("experience")),
                n.path("certifications").asText("Not clearly detected"), strings(n.path("strengths")),
                strings(n.path("gaps")), strings(n.path("improvements")), scores, overall,
                n.path("readiness").asText(overall >= 80 ? "Interview Ready" : overall >= 60 ? "Needs Targeted Practice" : "Needs Improvement"),
                true, "AI analysis completed using the local Ollama model."
        );
    }

    private List<String> strings(JsonNode node) {
        List<String> out = new ArrayList<>();
        if (node.isArray()) node.forEach(x -> out.add(x.asText()));
        else if (node.isTextual() && !node.asText().isBlank()) out.add(node.asText());
        return out;
    }

    private ResumeAnalysisResult heuristic(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        List<String> skills = detect(lower, text, new String[]{"Java","Spring Boot","Spring","SQL","MySQL","Python","C++","JavaScript","TypeScript","React","Angular","HTML","CSS","Git","GitHub","Docker","AWS","Azure","Hibernate","JPA","REST API","Data Structures","Machine Learning","TensorFlow","Salesforce"});
        List<String> projects = sectionLines(text, "projects", "project", "academic projects");
        List<String> experience = sectionLines(text, "experience", "work experience", "internship", "employment");
        String education = sectionText(text, "education", "academic background");
        String summary = sectionText(text, "summary", "profile", "objective", "about");
        String certifications = sectionText(text, "certifications", "certificates");
        Map<String,Integer> scores = new LinkedHashMap<>();
        scores.put("SKILLS", score(skills.size(), 8));
        scores.put("EDUCATION", scoreText(education));
        scores.put("PROJECTS", score(projects.size(), 3));
        scores.put("EXPERIENCE", score(experience.size(), 3));
        scores.put("SUMMARY", scoreText(summary));
        scores.put("CERTIFICATIONS", scoreText(certifications));
        int overall = (int)Math.round(scores.values().stream().mapToInt(Integer::intValue).average().orElse(0));
        List<String> strengths = new ArrayList<>();
        if (!skills.isEmpty()) strengths.add("Detected technical skills: " + String.join(", ", skills));
        if (!projects.isEmpty()) strengths.add("Projects section detected with " + projects.size() + " relevant entries.");
        if (!experience.isEmpty()) strengths.add("Experience section detected with " + experience.size() + " entries.");
        List<String> gaps = new ArrayList<>();
        if (skills.isEmpty()) gaps.add("Add a clearly labeled Technical Skills section.");
        if (projects.isEmpty()) gaps.add("Add 2-3 projects with technologies, responsibilities and measurable outcomes.");
        if (experience.isEmpty()) gaps.add("Add internship/work experience or clearly describe practical experience.");
        if (certifications.isBlank()) gaps.add("Add relevant certifications if you have them.");
        List<String> improvements = List.of("Use measurable results in project and experience bullets.", "Keep section headings explicit so recruiters and ATS tools can parse them.", "Tailor the top third of the resume to the target role.");
        return new ResumeAnalysisResult(summary.isBlank() ? "Resume text was extracted successfully; add a concise professional summary." : summary,
                education.isBlank() ? "Not clearly detected" : education,
                skills, projects, experience,
                certifications.isBlank() ? "Not clearly detected" : certifications,
                strengths, gaps, improvements, scores, overall,
                overall >= 80 ? "Interview Ready" : overall >= 60 ? "Needs Targeted Practice" : "Needs Improvement",
                false, "Ollama was unavailable, so a local resume parser generated this live fallback analysis. Start Ollama with: ollama serve, then keep llama3.2:3b installed for AI analysis.");
    }

    private List<String> detect(String lower, String original, String[] terms) {
        List<String> out = new ArrayList<>();
        for (String t : terms) if (lower.contains(t.toLowerCase(Locale.ROOT))) out.add(t);
        return out;
    }
    private int score(int count, int target) { return Math.min(100, (int)Math.round((count * 100.0) / target)); }
    private int scoreText(String s) { return s == null || s.isBlank() ? 20 : Math.min(100, 45 + Math.min(55, s.length()/8)); }

    private String sectionText(String text, String... headings) {
        String[] lines = text.split("\\R");
        for (int i=0;i<lines.length;i++) {
            String l=lines[i].trim().toLowerCase(Locale.ROOT);
            for(String h: headings) if(l.matches(".*\\b"+Pattern.quote(h)+"\\b.*")) {
                StringBuilder b=new StringBuilder();
                for(int j=i+1;j<Math.min(lines.length,i+8);j++) { String x=lines[j].trim(); if(x.matches("(?i)^[A-Z][A-Z &/]{2,}$")) break; if(!x.isBlank()) b.append(x).append(' '); }
                return b.toString().trim();
            }
        }
        return "";
    }
    private List<String> sectionLines(String text, String... headings) {
        List<String> out=new ArrayList<>(); String[] lines=text.split("\\R"); boolean in=false;
        for(String raw:lines){ String l=raw.trim(); String low=l.toLowerCase(Locale.ROOT);
            if(Arrays.stream(headings).anyMatch(h -> low.equals(h) || low.startsWith(h+":"))) {in=true; continue;}
            if(in && l.matches("(?i)^[A-Z][A-Z &/]{2,}$") && !Arrays.stream(headings).anyMatch(h->low.contains(h))) break;
            if(in && !l.isBlank()) out.add(l);
            if(out.size()>=8) break;
        }
        return out;
    }

    private String parse(MultipartFile file) {
        if(file==null || file.isEmpty()) throw new IllegalArgumentException("Resume file is required.");
        String name=file.getOriginalFilename()==null?"":file.getOriginalFilename().toLowerCase(Locale.ROOT);
        try {
            if(name.endsWith(".pdf") || "application/pdf".equalsIgnoreCase(file.getContentType())) {
                try(PDDocument doc=PDDocument.load(file.getInputStream())) { return new PDFTextStripper().getText(doc).trim(); }
            }
            if(name.endsWith(".docx") || "application/vnd.openxmlformats-officedocument.wordprocessingml.document".equalsIgnoreCase(file.getContentType())) {
                try(XWPFDocument doc=new XWPFDocument(file.getInputStream()); XWPFWordExtractor ex=new XWPFWordExtractor(doc)) { return ex.getText().trim(); }
            }
            if(name.endsWith(".txt") || (file.getContentType()!=null && file.getContentType().startsWith("text"))) {
                try(BufferedReader r=new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) { StringBuilder b=new StringBuilder(); String line; while((line=r.readLine())!=null)b.append(line).append('\n'); return b.toString().trim(); }
            }
            throw new IllegalArgumentException("Unsupported resume format. Upload PDF, DOCX or TXT.");
        } catch(IllegalArgumentException e){throw e;} catch(Exception e){throw new RuntimeException("Failed to read resume: "+e.getMessage(),e);}
    }
}
