package com.aicoach.controller;

import com.aicoach.dto.FutureFeatureDtos;
import com.aicoach.dto.ResumeAnalysisResult;
import com.aicoach.security.UserPrincipal;
import com.aicoach.service.FutureFeaturesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(value = "/api/future", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class FutureFeaturesController {

    private final FutureFeaturesService futureFeaturesService;

    @PostMapping("/voice")
    public FutureFeatureDtos.FeatureAdviceResponse voiceInterview(@AuthenticationPrincipal UserPrincipal principal,
                                                                  @Valid @RequestBody FutureFeatureDtos.VoiceInterviewRequest request) {
        return futureFeaturesService.voiceInterviewAdvice(request.targetRole(), request.sampleAnswer());
    }

    @PostMapping("/video")
    public FutureFeatureDtos.FeatureAdviceResponse videoInterview(@AuthenticationPrincipal UserPrincipal principal,
                                                                  @Valid @RequestBody FutureFeatureDtos.VideoInterviewRequest request) {
        return futureFeaturesService.videoInterviewAdvice(request.targetRole(), request.scenario());
    }

    @PostMapping("/company")
    public FutureFeatureDtos.FeatureAdviceResponse companyInterview(@AuthenticationPrincipal UserPrincipal principal,
                                                                    @Valid @RequestBody FutureFeatureDtos.CompanyInterviewRequest request) {
        return futureFeaturesService.companySpecificInterviewPrep(request.companyName(), request.targetRole());
    }

    @PostMapping("/coding")
    public FutureFeatureDtos.FeatureAdviceResponse codingInterview(@AuthenticationPrincipal UserPrincipal principal,
                                                                   @Valid @RequestBody FutureFeatureDtos.CodingInterviewRequest request) {
        return futureFeaturesService.codingInterviewPrep(request.language(), request.problemType(), request.experienceLevel());
    }

    @PostMapping("/group")
    public FutureFeatureDtos.FeatureAdviceResponse groupDiscussion(@AuthenticationPrincipal UserPrincipal principal,
                                                                   @Valid @RequestBody FutureFeatureDtos.GroupDiscussionRequest request) {
        return futureFeaturesService.groupDiscussionPrep(request.topic(), request.role());
    }

    @PostMapping("/resume")
    public FutureFeatureDtos.FeatureAdviceResponse resumeAnalysis(@AuthenticationPrincipal UserPrincipal principal,
                                                                   @Valid @RequestBody FutureFeatureDtos.ResumeAnalysisRequest request) {
        return futureFeaturesService.resumeAnalysis(request.resumeText());
    }

    @PostMapping(value = "/resume/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResumeAnalysisResult resumeAnalysisFile(@AuthenticationPrincipal UserPrincipal principal,
                                                   @RequestPart("file") MultipartFile file) {
        return futureFeaturesService.detailedResumeAnalysisFile(file);
    }

    @PostMapping("/prediction")
    public FutureFeatureDtos.CareerPredictionResponse careerPrediction(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody FutureFeatureDtos.CareerPredictionRequest request) {
        return futureFeaturesService.predictCareer(request.targetRole(), request.skills(), request.experience());
    }

    @PostMapping("/jobs")
    public FutureFeatureDtos.JobRecommendationResponse jobRecommendations(@AuthenticationPrincipal UserPrincipal principal,
                                                                          @Valid @RequestBody FutureFeatureDtos.JobRecommendationRequest request) {
        return futureFeaturesService.recommendJobs(request.targetRole(), request.skills());
    }
}
