package com.aicoach.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aicoach.dto.InterviewRequest;
import com.aicoach.dto.InterviewResponse;
import com.aicoach.service.CompanyService;

@RestController
@RequestMapping("/api/company")
@CrossOrigin(origins = "*")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @PostMapping("/question")
    public InterviewResponse question(
            @RequestBody InterviewRequest request) {

        String result =
                companyService.generateQuestion(
                        request.getCompany(),
                        request.getRole(),
                        request.getExperienceLevel()
                );

        return new InterviewResponse(
                true,
                result
        );
    }

    @PostMapping("/evaluate")
    public InterviewResponse evaluate(
            @RequestBody InterviewRequest request) {

        String result =
                companyService.evaluate(request);

        return new InterviewResponse(
                true,
                result
        );
    }
}