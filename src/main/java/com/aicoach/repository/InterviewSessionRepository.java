package com.aicoach.repository;

import com.aicoach.entity.InterviewSession;
import com.aicoach.entity.InterviewSession.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InterviewSessionRepository
        extends JpaRepository<InterviewSession, Long> {

    Optional<InterviewSession> findByIdAndUserId(
            Long id,
            Long userId
    );

    List<InterviewSession> findByUserIdOrderByStartedAtDesc(
            Long userId
    );

    long countByUserIdAndStatus(
            Long userId,
            SessionStatus status
    );
}
//src/main/java/com/aicoach/dto/ChatRequest.java