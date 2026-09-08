package com.aicoach.controller;

import com.aicoach.dto.InterviewRequest;
import com.aicoach.dto.InterviewResponse;
import com.aicoach.service.VoiceService;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/voice")
@CrossOrigin(origins = "*")
public class VoiceController {

    private final VoiceService voiceService;

    public VoiceController(VoiceService voiceService) {
        this.voiceService = voiceService;
    }

    @PostMapping("/question")
    public InterviewResponse question(
            @RequestBody InterviewRequest request) {

        String result =
                voiceService.generateQuestion(
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
                voiceService.evaluateAnswer(request);

        return new InterviewResponse(
                true,
                result
        );
    }
}