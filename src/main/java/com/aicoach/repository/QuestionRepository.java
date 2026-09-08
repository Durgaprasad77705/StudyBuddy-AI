package com.aicoach.repository;

import com.aicoach.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    Optional<Question> findByIdAndSessionId(Long id, Long sessionId);
}
