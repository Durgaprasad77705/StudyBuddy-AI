package com.aicoach.controller;

import com.aicoach.dto.InterviewResponse;
import com.aicoach.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/group")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @PostMapping("/topic")
    public InterviewResponse topic(@RequestBody Map<String, String> request) {
        String result = groupService.generateTopic(request.get("role"), request.get("experience"));
        return new InterviewResponse(true, result);
    }

    @PostMapping("/topics")
    public Map<String, Object> topics(@RequestBody Map<String, String> request) {
        List<String> topics = groupService.generateTopics(
                request.get("role"),
                request.get("experience"),
                10
        );
        return Map.of("success", true, "topics", topics);
    }

    @PostMapping("/participant")
    public InterviewResponse participant(@RequestBody Map<String, String> request) {
        String result = groupService.generateParticipantResponse(
                request.get("topic"),
                request.get("candidateStatement"),
                request.get("persona")
        );
        return new InterviewResponse(true, result);
    }

    @PostMapping("/evaluate")
    public InterviewResponse evaluate(@RequestBody Map<String, String> request) {
        String result = groupService.evaluateDiscussion(
                request.get("topic"),
                request.get("transcript")
        );
        return new InterviewResponse(true, result);
    }
}
