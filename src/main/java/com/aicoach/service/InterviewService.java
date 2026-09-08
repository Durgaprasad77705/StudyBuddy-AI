package com.aicoach.service;

import com.aicoach.dto.InterviewDtos.*;
import com.aicoach.entity.*;
import com.aicoach.entity.InterviewSession.SessionStatus;
import com.aicoach.exception.BadRequestException;
import com.aicoach.exception.ResourceNotFoundException;
import com.aicoach.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InterviewService {

    private static final int QUESTIONS_PER_SESSION = 5;

    private final InterviewSessionRepository sessionRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final AiCoachService aiCoachService;
    private final GamificationService gamificationService;


    // ============================================================
    // START SESSION
    // ============================================================

    @Transactional
    public SessionResponse startSession(
            Long userId,
            StartSessionRequest request
    ) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found")
                );

        Profile profile = profileRepository
                .findByUserId(userId)
                .orElse(null);

        Profile.ExperienceLevel level =
                profile != null && profile.getExperienceLevel() != null
                        ? profile.getExperienceLevel()
                        : Profile.ExperienceLevel.ENTRY;

        String targetRole =
                request.targetRole() != null &&
                !request.targetRole().isBlank()
                        ? request.targetRole()
                        : profile != null
                            ? profile.getTargetRole()
                            : null;

        InterviewSession session = InterviewSession.builder()
                .user(user)
                .type(request.type())
                .targetRole(targetRole)
                .timed(request.timed())
                .status(SessionStatus.IN_PROGRESS)
                .build();

        session = sessionRepository.save(session);

        List<String> questionTexts =
                aiCoachService.generateQuestions(
                        request.type(),
                        targetRole,
                        level,
                        QUESTIONS_PER_SESSION
                );

        int index = 0;

        for (String text : questionTexts) {

            Question question = Question.builder()
                    .session(session)
                    .text(text)
                    .category(request.type().name())
                    .orderIndex(index++)
                    .build();

            session.getQuestions().add(question);
        }

        session = sessionRepository.save(session);

        return toSessionResponse(session);
    }


    // ============================================================
    // GET SESSION
    // ============================================================

    public SessionResponse getSession(
            Long userId,
            Long sessionId
    ) {

        return toSessionResponse(
                getOwnedSession(userId, sessionId)
        );
    }


    // ============================================================
    // SUBMIT NORMAL ANSWER
    // ============================================================

    @Transactional
    public QuestionFeedback submitAnswer(
            Long userId,
            Long sessionId,
            SubmitAnswerRequest request
    ) {

        InterviewSession session =
                getOwnedSession(userId, sessionId);

        if (session.getStatus() != SessionStatus.IN_PROGRESS) {

            throw new BadRequestException(
                    "This interview session is not in progress"
            );
        }

        Question question =
                questionRepository
                        .findByIdAndSessionId(
                                request.questionId(),
                                sessionId
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Question not found in this session"
                                )
                        );

        String answerText = request.answerText();

        if (answerText == null || answerText.isBlank()) {

            throw new BadRequestException(
                    "Answer cannot be empty"
            );
        }

        AiCoachService.AnswerEvaluation evaluation =
                aiCoachService.evaluateAnswer(
                        question.getText(),
                        answerText,
                        session.getType()
                );

        Answer answer = question.getAnswer();

        if (answer == null) {

            answer = Answer.builder()
                    .question(question)
                    .build();

            question.setAnswer(answer);
        }

        answer.setAnswerText(answerText);
        answer.setTimeTakenSeconds(
                request.timeTakenSeconds()
        );
        answer.setAiScore(
                evaluation.score()
        );
        answer.setAiFeedback(
                evaluation.feedback()
        );

        answerRepository.save(answer);

        return new QuestionFeedback(
                question.getId(),
                question.getText(),
                answerText,
                evaluation.score(),
                evaluation.feedback()
        );
    }


    // ============================================================
    // LIVE ANSWER
    // ============================================================

    @Transactional
    public LiveAnswerResponse submitLiveAnswer(
            Long userId,
            Long sessionId,
            LiveAnswerRequest request
    ) {

        InterviewSession session =
                getOwnedSession(userId, sessionId);

        if (session.getStatus() != SessionStatus.IN_PROGRESS) {

            throw new BadRequestException(
                    "This interview session is not in progress"
            );
        }

        Question question =
                questionRepository
                        .findByIdAndSessionId(
                                request.questionId(),
                                sessionId
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Question not found in this session"
                                )
                        );

        String transcript = request.transcript();

        if (transcript == null || transcript.isBlank()) {

            throw new BadRequestException(
                    "Transcript cannot be empty"
            );
        }

        transcript = transcript.trim();

        AiCoachService.AnswerEvaluation evaluation =
                aiCoachService.evaluateAnswer(
                        question.getText(),
                        transcript,
                        session.getType()
                );

        Answer answer = question.getAnswer();

        if (answer == null) {

            answer = Answer.builder()
                    .question(question)
                    .build();

            question.setAnswer(answer);
        }

        answer.setAnswerText(transcript);

        answer.setTimeTakenSeconds(
                request.timeTakenSeconds()
        );

        answer.setAiScore(
                evaluation.score()
        );

        answer.setAiFeedback(
                evaluation.feedback()
        );

        answerRepository.save(answer);

        int totalQuestions =
                session.getQuestions().size();

        int currentQuestionIndex =
                session.getQuestions().indexOf(question);

        boolean completed =
                currentQuestionIndex + 1 >= totalQuestions;

        Question nextQuestion =
                completed
                        ? null
                        : session.getQuestions()
                            .get(currentQuestionIndex + 1);

        if (nextQuestion != null) {

            String followUp =
                    aiCoachService.generateFollowUpQuestion(
                            session.getType(),
                            session.getTargetRole(),
                            question.getText(),
                            transcript
                    );

            nextQuestion.setText(followUp);

            questionRepository.save(nextQuestion);
        }

        int fillerWords =
                countFillerWords(transcript);

        int wordCount =
                transcript.split("\\s+").length;

        double pace = 0.0;

        if (request.timeTakenSeconds() != null &&
                request.timeTakenSeconds() > 0) {

            pace =
                    Math.round(
                            (wordCount /
                                    request.timeTakenSeconds())
                                    * 60.0
                                    * 10.0
                    ) / 10.0;
        }

        return new LiveAnswerResponse(
                question.getId(),
                question.getText(),
                evaluation.score(),
                evaluation.feedback(),
                nextQuestion != null
                        ? nextQuestion.getId()
                        : null,
                nextQuestion != null
                        ? nextQuestion.getText()
                        : null,
                currentQuestionIndex + 2,
                totalQuestions,
                completed,
                wordCount,
                fillerWords,
                pace,
                "Processed answer and generated the next question."
        );
    }


    // ============================================================
    // LIVE STATUS
    // ============================================================

    public LiveStatusResponse getLiveStatus(
            Long userId,
            Long sessionId
    ) {

        InterviewSession session =
                getOwnedSession(userId, sessionId);

        Question current =
                session.getQuestions()
                        .stream()
                        .filter(q -> q.getAnswer() == null)
                        .findFirst()
                        .orElse(null);

        int currentIndex;

        if (current == null) {
            currentIndex =
                    session.getQuestions().size();
        } else {
            currentIndex =
                    session.getQuestions().indexOf(current);
        }

        boolean completed =
                session.getStatus() == SessionStatus.COMPLETED;

        return new LiveStatusResponse(
                session.getId(),
                completed
                        ? "COMPLETED"
                        : "READY",
                current != null
                        ? current.getId()
                        : null,
                current != null
                        ? current.getText()
                        : null,
                currentIndex + 1,
                session.getQuestions().size(),
                completed,
                completed
                        ? "Interview completed"
                        : "Waiting for candidate response"
        );
    }


    // ============================================================
    // FILLER WORDS
    // ============================================================

    private int countFillerWords(String transcript) {

        String[] fillers = {
                "um",
                "uh",
                "like",
                "you know",
                "so",
                "actually",
                "basically",
                "right"
        };

        String lower =
                transcript.toLowerCase();

        int count = 0;

        for (String filler : fillers) {

            int index = 0;

            while ((index =
                    lower.indexOf(filler, index)) >= 0) {

                count++;

                index += filler.length();
            }
        }

        return count;
    }


    // ============================================================
    // COMPLETE SESSION
    // ============================================================

    @Transactional
    public ReportResponse completeSession(
            Long userId,
            Long sessionId
    ) {

        // 1. Get interview session
        InterviewSession session =
                getOwnedSession(userId, sessionId);


        // 2. If already completed, return existing report.
        //    This prevents duplicate feedback insertion.
        if (session.getStatus() == SessionStatus.COMPLETED) {

            return buildReport(session);
        }


        // 3. Collect answered questions
        List<AiCoachService.QaPair> qaPairs =
                session.getQuestions()
                        .stream()
                        .filter(q -> q.getAnswer() != null)
                        .filter(q ->
                                q.getAnswer().getAnswerText() != null)
                        .filter(q ->
                                !q.getAnswer()
                                        .getAnswerText()
                                        .isBlank())
                        .map(q ->
                                new AiCoachService.QaPair(
                                        q.getText(),
                                        q.getAnswer()
                                                .getAnswerText()
                                )
                        )
                        .collect(Collectors.toList());


        // 4. Do not complete an empty interview
        if (qaPairs.isEmpty()) {

            throw new BadRequestException(
                    "Answer at least one question before completing the interview"
            );
        }


        // 5. AI evaluates complete interview
        AiCoachService.SessionEvaluation eval =
                aiCoachService.evaluateSession(
                        session.getType(),
                        session.getTargetRole(),
                        qaPairs
                );


        // 6. Update scores
        session.setCommunicationScore(
                eval.communicationScore()
        );

        session.setConfidenceScore(
                eval.confidenceScore()
        );

        session.setKnowledgeScore(
                eval.knowledgeScore()
        );

        session.setOverallScore(
                eval.overallScore()
        );


        // 7. Find existing feedback FIRST
        Feedback feedback =
                feedbackRepository
                        .findBySessionId(session.getId())
                        .orElse(null);


        // 8. Create feedback ONLY if it does not exist
        if (feedback == null) {

            feedback =
                    Feedback.builder()
                            .session(session)
                            .build();
        }


        // 9. Update feedback
        feedback.setStrengths(
                String.join(
                        "|",
                        eval.strengths()
                )
        );

        feedback.setWeaknesses(
                String.join(
                        "|",
                        eval.weaknesses()
                )
        );

        feedback.setImprovementPlan(
                String.join(
                        "|",
                        eval.improvementPlan()
                )
        );

        feedback.setSummary(
                eval.summary()
        );


        // 10. Save feedback
        feedback =
                feedbackRepository.save(feedback);


        // 11. Link feedback with session
        session.setFeedback(feedback);


        // 12. Mark session completed
        session.setStatus(
                SessionStatus.COMPLETED
        );

        session.setCompletedAt(
                LocalDateTime.now()
        );


        // 13. Save session
        sessionRepository.save(session);


        // 14. Gamification
        gamificationService.onSessionCompleted(
                userId,
                eval.overallScore()
        );


        // 15. Return final report
        return buildReport(session);
    }


    // ============================================================
    // GET REPORT
    // ============================================================

    public ReportResponse getReport(
            Long userId,
            Long sessionId
    ) {

        InterviewSession session =
                getOwnedSession(userId, sessionId);

        if (session.getStatus() != SessionStatus.COMPLETED) {

            throw new BadRequestException(
                    "This interview has not been completed yet"
            );
        }

        return buildReport(session);
    }


    // ============================================================
    // HISTORY
    // ============================================================

    public List<HistoryItem> getHistory(
            Long userId
    ) {

        return sessionRepository
                .findByUserIdOrderByStartedAtDesc(userId)
                .stream()
                .map(s ->
                        new HistoryItem(
                                s.getId(),
                                s.getType(),
                                s.getTargetRole(),
                                s.getStatus(),
                                s.getStartedAt(),
                                s.getCompletedAt(),
                                s.getOverallScore()
                        )
                )
                .collect(Collectors.toList());
    }


    // ============================================================
    // PROGRESS
    // ============================================================

    public ProgressResponse getProgress(
            Long userId
    ) {

        List<InterviewSession> completed =
                sessionRepository
                        .findByUserIdOrderByStartedAtDesc(userId)
                        .stream()
                        .filter(s ->
                                s.getStatus()
                                        == SessionStatus.COMPLETED)
                        .filter(s ->
                                s.getCompletedAt() != null)
                        .sorted(
                                (a, b) ->
                                        a.getCompletedAt()
                                                .compareTo(
                                                        b.getCompletedAt()
                                                )
                        )
                        .toList();


        List<ProgressPoint> points =
                completed
                        .stream()
                        .map(s ->
                                new ProgressPoint(
                                        s.getId(),
                                        s.getCompletedAt(),
                                        s.getOverallScore(),
                                        s.getCommunicationScore(),
                                        s.getConfidenceScore(),
                                        s.getKnowledgeScore()
                                )
                        )
                        .collect(Collectors.toList());


        double avg =
                points
                        .stream()
                        .mapToDouble(
                                ProgressPoint::overallScore
                        )
                        .average()
                        .orElse(0);


        double trend =
                points.size() >= 2
                        ? points.get(
                                points.size() - 1
                        ).overallScore()
                        -
                        points.get(0).overallScore()
                        : 0;


        UserGamification gamification =
                gamificationService.getOrCreate(userId);


        return new ProgressResponse(
                points,
                round(avg),
                round(trend),
                gamification.getCurrentStreak(),
                gamification.getLongestStreak()
        );
    }


    // ============================================================
    // GET OWNED SESSION
    // ============================================================

    private InterviewSession getOwnedSession(
            Long userId,
            Long sessionId
    ) {

        return sessionRepository
                .findByIdAndUserId(
                        sessionId,
                        userId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Interview session not found"
                        )
                );
    }


    // ============================================================
    // SESSION RESPONSE
    // ============================================================

    private SessionResponse toSessionResponse(
            InterviewSession session
    ) {

        List<QuestionResponse> questions =
                session.getQuestions()
                        .stream()
                        .map(q ->
                                new QuestionResponse(
                                        q.getId(),
                                        q.getText(),
                                        q.getCategory(),
                                        q.getOrderIndex(),
                                        q.getAnswer() != null
                                )
                        )
                        .collect(Collectors.toList());

        return new SessionResponse(
                session.getId(),
                session.getType(),
                session.getTargetRole(),
                session.isTimed(),
                session.getStatus(),
                session.getStartedAt(),
                questions
        );
    }


    // ============================================================
    // BUILD REPORT
    // ============================================================

    private ReportResponse buildReport(
            InterviewSession session
    ) {

        Feedback feedback =
                session.getFeedback();


        // If Feedback is not loaded through the relationship,
        // retrieve it directly.
        if (feedback == null) {

            feedback =
                    feedbackRepository
                            .findBySessionId(
                                    session.getId()
                            )
                            .orElse(null);
        }


        List<QuestionFeedback> questionFeedback =
                session.getQuestions()
                        .stream()
                        .filter(q ->
                                q.getAnswer() != null)
                        .map(q -> {

                            Double score =
                                    q.getAnswer()
                                            .getAiScore();

                            return new QuestionFeedback(
                                    q.getId(),
                                    q.getText(),
                                    q.getAnswer()
                                            .getAnswerText(),
                                    score != null
                                            ? score
                                            : 0,
                                    q.getAnswer()
                                            .getAiFeedback()
                            );
                        })
                        .collect(Collectors.toList());


        return new ReportResponse(
                session.getId(),
                session.getType(),
                session.getTargetRole(),
                session.getStartedAt(),
                session.getCompletedAt(),

                nz(session.getOverallScore()),
                nz(session.getCommunicationScore()),
                nz(session.getConfidenceScore()),
                nz(session.getKnowledgeScore()),

                splitOrEmpty(
                        feedback != null
                                ? feedback.getStrengths()
                                : null
                ),

                splitOrEmpty(
                        feedback != null
                                ? feedback.getWeaknesses()
                                : null
                ),

                splitOrEmpty(
                        feedback != null
                                ? feedback.getImprovementPlan()
                                : null
                ),

                feedback != null
                        ? feedback.getSummary()
                        : null,

                questionFeedback
        );
    }


    // ============================================================
    // SPLIT FEEDBACK
    // ============================================================

    private List<String> splitOrEmpty(
            String pipeSeparated
    ) {

        if (pipeSeparated == null ||
                pipeSeparated.isBlank()) {

            return List.of();
        }

        return List.of(
                pipeSeparated.split("\\|")
        );
    }


    // ============================================================
    // NULL-SAFE DOUBLE
    // ============================================================

    private double nz(Double value) {

        return value != null
                ? value
                : 0.0;
    }


    // ============================================================
    // ROUND
    // ============================================================

    private double round(double value) {

        return Math.round(
                value * 10.0
        ) / 10.0;
    }
}