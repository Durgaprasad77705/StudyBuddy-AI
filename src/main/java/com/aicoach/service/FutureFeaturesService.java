package com.aicoach.service;

import com.aicoach.dto.ResumeAnalysisResult;

import com.aicoach.dto.FutureFeatureDtos.FeatureAdviceResponse;
import com.aicoach.dto.FutureFeatureDtos.JobRecommendationResponse;
import com.aicoach.dto.FutureFeatureDtos;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FutureFeaturesService {

    FeatureAdviceResponse voiceInterviewAdvice(String targetRole, String sampleAnswer);

    FeatureAdviceResponse videoInterviewAdvice(String targetRole, String scenario);

    FeatureAdviceResponse companySpecificInterviewPrep(String companyName, String targetRole);

    FeatureAdviceResponse codingInterviewPrep(String language, String problemType, String experienceLevel);

    FeatureAdviceResponse groupDiscussionPrep(String topic, String role);

    FeatureAdviceResponse resumeAnalysis(String resumeText);

    FeatureAdviceResponse resumeAnalysisFile(MultipartFile file);

    ResumeAnalysisResult detailedResumeAnalysisFile(MultipartFile file);


    JobRecommendationResponse recommendJobs(String targetRole, List<String> skills);

    FutureFeatureDtos.CareerPredictionResponse predictCareer(String targetRole, List<String> skills, String experience);
}
