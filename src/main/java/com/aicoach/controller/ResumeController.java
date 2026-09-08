package com.aicoach.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aicoach.dto.ResumeAnalysisResponse;
import com.aicoach.service.ResumeService;

@RestController
@RequestMapping("/api/resume")
@CrossOrigin(origins = "*")
public class ResumeController {

    private final ResumeService resumeService;

    public ResumeController(ResumeService resumeService) {
        this.resumeService = resumeService;
    }

    @PostMapping("/analyze")
    public ResponseEntity<ResumeAnalysisResponse> analyzeResume(
            @RequestBody ResumeRequest request) {

        try {

            String result =
                    resumeService.analyzeResume(
                            request.getResumeText()
                    );

            return ResponseEntity.ok(
                    new ResumeAnalysisResponse(
                            true,
                            result
                    )
            );

        } catch (Exception e) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            new ResumeAnalysisResponse(
                                    false,
                                    e.getMessage()
                            )
                    );
        }
    }


    public static class ResumeRequest {

        private String resumeText;

        public ResumeRequest() {
        }

        public String getResumeText() {
            return resumeText;
        }

        public void setResumeText(String resumeText) {
            this.resumeText = resumeText;
        }
    }
}