package com.aicoach.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aicoach.dto.CodingRequest;
import com.aicoach.service.CodingService;

@RestController
@RequestMapping("/api/coding")
@CrossOrigin(origins = "*")
public class CodingController {

    private final CodingService codingService;

    public CodingController(CodingService codingService) {
        this.codingService = codingService;
    }

    @PostMapping("/generate")
    public Map<String, String> generate(
            @RequestBody CodingRequest request) {

        String result = codingService.generateProblem(
                request.getLanguage(),
                request.getProblemType(),
                request.getExperienceLevel()
        );

        return Map.of("result", result);
    }

    @PostMapping("/evaluate")
    public Map<String, String> evaluate(
            @RequestBody Map<String, String> request) {

        String result = codingService.evaluateCode(
                request.get("problem"),
                request.get("code"),
                request.get("language")
        );

        return Map.of("result", result);
    }
}