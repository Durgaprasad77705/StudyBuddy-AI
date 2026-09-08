package com.aicoach.controller;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aicoach.dto.InterviewDtos.HistoryItem;
import com.aicoach.dto.InterviewDtos.LiveAnswerRequest;
import com.aicoach.dto.InterviewDtos.LiveAnswerResponse;
import com.aicoach.dto.InterviewDtos.LiveStatusResponse;
import com.aicoach.dto.InterviewDtos.ProgressResponse;
import com.aicoach.dto.InterviewDtos.QuestionFeedback;
import com.aicoach.dto.InterviewDtos.ReportResponse;
import com.aicoach.dto.InterviewDtos.SessionResponse;
import com.aicoach.dto.InterviewDtos.StartSessionRequest;
import com.aicoach.dto.InterviewDtos.SubmitAnswerRequest;
import com.aicoach.security.UserPrincipal;
import com.aicoach.service.InterviewService;
import com.aicoach.service.ReportPdfService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/interviews")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;
    private final ReportPdfService reportPdfService;

    // Phase 2: choose interview type -> AI generates questions
    @PostMapping("/start")
    public SessionResponse start(@AuthenticationPrincipal UserPrincipal principal,
                                  @Valid @RequestBody StartSessionRequest request) {
        return interviewService.startSession(principal.getUserId(), request);
    }

    @GetMapping("/{sessionId}")
    public SessionResponse getSession(@AuthenticationPrincipal UserPrincipal principal,
                                       @PathVariable Long sessionId) {
        return interviewService.getSession(principal.getUserId(), sessionId);
    }

    // Phase 2/3: submit one answer -> immediate AI evaluation (question-wise feedback)
    @PostMapping("/{sessionId}/answers")
    public QuestionFeedback submitAnswer(@AuthenticationPrincipal UserPrincipal principal,
                                          @PathVariable Long sessionId,
                                          @Valid @RequestBody SubmitAnswerRequest request) {
        return interviewService.submitAnswer(principal.getUserId(), sessionId, request);
    }

    // Phase 5/6: live voice/video interview answer submission
    @PostMapping("/{sessionId}/voice-answer")
    public LiveAnswerResponse submitLiveAnswer(@AuthenticationPrincipal UserPrincipal principal,
                                               @PathVariable Long sessionId,
                                               @Valid @RequestBody LiveAnswerRequest request) {
        return interviewService.submitLiveAnswer(principal.getUserId(), sessionId, request);
    }

    @GetMapping("/{sessionId}/live-status")
    public LiveStatusResponse liveStatus(@AuthenticationPrincipal UserPrincipal principal,
                                         @PathVariable Long sessionId) {
        return interviewService.getLiveStatus(principal.getUserId(), sessionId);
    }

    // Phase 3: complete -> holistic AI evaluation + report + gamification XP
    @PostMapping("/{sessionId}/complete")
    public ReportResponse complete(@AuthenticationPrincipal UserPrincipal principal,
                                    @PathVariable Long sessionId) {
        return interviewService.completeSession(principal.getUserId(), sessionId);
    }

    // Phase 3: interview report
    @GetMapping("/{sessionId}/report")
    public ReportResponse getReport(@AuthenticationPrincipal UserPrincipal principal,
                                     @PathVariable Long sessionId) {
        return interviewService.getReport(principal.getUserId(), sessionId);
    }

    // Phase 3: downloadable PDF report
    @GetMapping("/{sessionId}/report/pdf")
    public ResponseEntity<byte[]> getReportPdf(@AuthenticationPrincipal UserPrincipal principal,
                                                @PathVariable Long sessionId) {
        ReportResponse report = interviewService.getReport(principal.getUserId(), sessionId);
        byte[] pdf = reportPdfService.generate(report);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=interview-report-" + sessionId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    // Phase 4: interview history
    @GetMapping("/history")
    public List<HistoryItem> history(@AuthenticationPrincipal UserPrincipal principal) {
        return interviewService.getHistory(principal.getUserId());
    }

    // Phase 4: progress tracking
    @GetMapping("/progress")
    public ProgressResponse progress(@AuthenticationPrincipal UserPrincipal principal) {
        return interviewService.getProgress(principal.getUserId());
    }
}
//service/