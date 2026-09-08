package com.aicoach.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aicoach.dto.InterviewRequest;
import com.aicoach.dto.InterviewResponse;
import com.aicoach.service.VideoService;

@RestController
@RequestMapping("/api/video")
@CrossOrigin(origins = "*")
public class VideoController {

    private final VideoService videoService;

    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    @PostMapping("/question")
    public InterviewResponse question(
            @RequestBody InterviewRequest request) {

        String result =
                videoService.generateQuestion(
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
                videoService.evaluateTranscript(
                        request
                );

        return new InterviewResponse(
                true,
                result
        );
    }
}