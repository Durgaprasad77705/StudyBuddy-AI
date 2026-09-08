package com.aicoach.controller;

import com.aicoach.dto.InterviewResponse;
import com.aicoach.dto.JobSearchResponse;
import com.aicoach.service.JobsService;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/jobs")
@CrossOrigin(origins="*")
public class JobsController {
 private final JobsService jobsService; public JobsController(JobsService jobsService){this.jobsService=jobsService;}
 @PostMapping("/analyze") public InterviewResponse analyze(@RequestBody Map<String,String> request){return new InterviewResponse(true,jobsService.analyzeJob(request.get("resumeText"),request.get("jobDescription")));}
 @PostMapping("/search") public JobSearchResponse search(@RequestBody Map<String,String> request){return jobsService.liveSearch(request.get("query"),request.get("location"),request.get("experience"));}
 @PostMapping("/precision") public Map<String,String> precision(@RequestBody Map<String,String> request){return Map.of("analysis", jobsService.precision(request.get("query"), request.get("location"), request.get("experience")));}
}
